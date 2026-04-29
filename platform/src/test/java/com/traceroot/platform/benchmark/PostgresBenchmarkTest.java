package com.traceroot.platform.benchmark;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL query benchmarks against the 10M-row logs table.
 * <p>
 * Runs five queries representing the most common log analysis workloads:
 * 1. Structured filter — baseline, your current workload
 * 2. Timestamp range aggregation — "error rate per hour"
 * 3. High-cardinality groupby — "count by (service, endpoint, minute)"
 * 4. Full-text search — LIKE on message field
 * 5. Distinct cardinality — distinct trace IDs per service
 * <p>
 * Each query:
 * - Runs 3 times, takes the median time (reduces noise)
 * - Captures the EXPLAIN ANALYZE plan
 * - Writes full detail to benchmark-output/
 * <p>
 * Tagged "benchmark" — excluded from default mvn test runs. Run explicitly:
 * mvn test -Dtest=PostgresBenchmarkTest -Dspring.profiles.active=benchmark
 * <p>
 * Important:
 * - Assumes the logs table has been seeded (run LogSeedRunner first).
 * - Runs against traceroot_benchmark, not traceroot (profile isolation).
 * - The real signal in these numbers is relative ranking, not absolute.
 * Running on your laptop vs. a fast SSD server changes absolute numbers
 * dramatically, but the ratio between queries stays stable.
 */
@SpringBootTest
@ActiveProfiles({"benchmark", "stub"})
@Tag("benchmark")
public class PostgresBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(PostgresBenchmarkTest.class);
    private static final int RUNS_PER_QUERY = 3;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void runAllBenchmarks() throws Exception {
        BenchmarkResultWriter writer = new BenchmarkResultWriter(LocalDateTime.now());
        log.info("Benchmark run. Output folder: {}", writer.getRunFolder());

        Map<String, Object> rowCounts = captureRowCounts();
        log.info("Row counts: {}", rowCounts);

        List<Map<String, Object>> summaries = new ArrayList<>();

        //Q1: Structured filter — baseline workload
        summaries.add(runQuery(writer, 1, "Structured filter",
                "SELECT id, service_name, level, timestamp, message " +
                        "FROM logs WHERE service_name = ? AND level = ? LIMIT 100",
                new Object[]{"api-gateway", "ERROR"}));

        //Q2: Time range aggregation, hourly buckets
        summaries.add(runQuery(writer, 2, "Time range aggregation",
                "SELECT date_trunc('hour', timestamp) AS hour, COUNT(*) " +
                        "FROM logs " +
                        "WHERE timestamp > NOW() - INTERVAL '24 hours' AND level = ? " +
                        "GROUP BY hour ORDER BY hour",
                new Object[]{"ERROR"}));

        //Q2a: Time range aggregation, minute buckets
        // Same time window as Q2 but per-minute resolution.
        // Forces ~1440 result rows, each requiring its own grouping.
        // Realistic for real-time dashboards that display fine-grained traffic.
        summaries.add(runQuery(writer, 21, "Time range aggregation per-minute",
                "SELECT date_trunc('minute', timestamp) AS minute, level, COUNT(*) " +
                        "FROM logs " +
                        "WHERE timestamp > NOW() - INTERVAL '24 hours' " +
                        "GROUP BY minute, level " +
                        "ORDER BY minute",
                new Object[]{}));

        //Q3: High-cardinality groupby
        summaries.add(runQuery(writer, 3, "High-cardinality groupby",
                "SELECT service_name, endpoint, date_trunc('minute', timestamp) AS minute, COUNT(*) " +
                        "FROM logs " +
                        "WHERE timestamp > NOW() - INTERVAL '24 hour' AND level = ? " +
                        "GROUP BY service_name, endpoint, minute " +
                        "ORDER BY COUNT(*) DESC LIMIT 100",
                new Object[]{"ERROR"}));

        //Q4: Full-text search with LIMIT and common term
        //LIMIT 100 + common term means PostgreSQL short-circuits early.
        //This is the "best case" for full-text search.
        summaries.add(runQuery(writer, 4, "Full-text search",
                "SELECT id, service_name, message, timestamp " +
                        "FROM logs WHERE message ILIKE ? LIMIT 100",
                new Object[]{"%connection%"}));

        //Q4a: Full-text search without LIMIT
        //COUNT(*) forces a full scan. No LIMIT short-circuit available.
        // This is what unbounded full-text aggregation actually costs.
        summaries.add(runQuery(writer, 41, "Full-text search count all",
                "SELECT COUNT(*) FROM logs WHERE message ILIKE ?",
                new Object[]{"%connection%"}));


        //Q4b: Full-text search with rare term (PostgreSQL must scan deep into the table before)
        //finding 100 matches. LIMIT doesn't save you when matches are sparse.
        summaries.add(runQuery(writer, 42, "Full-text search rare term",
                "SELECT id, service_name, message, timestamp " +
                        "FROM logs WHERE message ILIKE ? LIMIT 100",
                new Object[]{"%rate limit%"}));


        summaries.add(runQuery(writer, 5, "Distinct cardinality",
                "SELECT service_name, COUNT(DISTINCT trace_id) AS unique_traces " +
                        "FROM logs " +
                        "WHERE timestamp > NOW() - INTERVAL '24 hours' " +
                        "GROUP BY service_name",
                new Object[]{}));

        writer.writeIndex(rowCounts, summaries);
        log.info("Benchmark complete. Results in: {}", writer.getRunFolder());
    }

    /**
     * Runs a single query RUNS_PER_QUERY times, captures median time and
     * EXPLAIN ANALYZE plan, writes detail to disk, returns summary map.
     */
    private Map<String, Object> runQuery(
            BenchmarkResultWriter writer,
            int queryNumber,
            String queryName,
            String sql,
            Object[] params) throws Exception {

        log.info("Q{}: {} — running {} iterations", queryNumber, queryName, RUNS_PER_QUERY);

        List<Long> timingsMs = new ArrayList<>();
        int rowCount = 0;

        for (int i = 0; i < RUNS_PER_QUERY; i++) {
            long startNanos = System.nanoTime();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            timingsMs.add(elapsedMs);
            rowCount = results.size();
            log.debug("  run {}: {}ms, {} rows", i + 1, elapsedMs, rowCount);
        }

        long medianMs = median(timingsMs);
        log.info("Q{}: {} — median {}ms ({} rows returned)",
                queryNumber, queryName, medianMs, rowCount);

        // Capture the query plan. EXPLAIN ANALYZE actually runs the query
        // again, but we only capture it once (not in the timing loop).
        String explainPlan = captureExplainPlan(sql, params);

        // Build detailed result for disk.
        Map<String, Object> detailed = new LinkedHashMap<>();
        detailed.put("queryNumber", queryNumber);
        detailed.put("queryName", queryName);
        detailed.put("sql", sql);
        detailed.put("params", params);
        detailed.put("timingsMs", timingsMs);
        detailed.put("medianMs", medianMs);
        detailed.put("rowsReturned", rowCount);
        detailed.put("explainPlan", explainPlan);
        writer.writeQueryResult(queryNumber, queryName, detailed);

        // Return summary for index.
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("queryNumber", queryNumber);
        summary.put("queryName", queryName);
        summary.put("medianMs", medianMs);
        summary.put("rowsReturned", rowCount);
        return summary;
    }

    /**
     * Captures the query's EXPLAIN ANALYZE plan as a single string.
     * The plan shows PostgreSQL's chosen strategy — sequential scan,
     * index scan, sort, hash aggregate, etc. Critical for the article:
     * readers see WHY the query is slow, not just that it is.
     */
    private String captureExplainPlan(String sql, Object[] params) {
        String explainSql = "EXPLAIN ANALYZE " + sql;
        List<String> planLines = jdbcTemplate.queryForList(explainSql, String.class, params);
        return String.join("\n", planLines);
    }

    /**
     * Captures basic facts about the data: total row count, distinct
     * services, distinct trace IDs. Shown in the index file so results
     * are self-describing — a reader can tell what scale this benchmark
     * ran at without needing external context.
     */
    private Map<String, Object> captureRowCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("totalLogs", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs", Long.class));
        counts.put("distinctServices", jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT service_name) FROM logs", Long.class));
        counts.put("distinctEndpoints", jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT endpoint) FROM logs", Long.class));
        counts.put("errorCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs WHERE level = 'ERROR'", Long.class));
        return counts;
    }

    private long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        return sorted.get(sorted.size() / 2);
    }
}