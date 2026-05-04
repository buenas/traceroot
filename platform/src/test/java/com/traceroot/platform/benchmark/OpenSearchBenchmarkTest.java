package com.traceroot.platform.benchmark;

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch query benchmarks against the same 10M-row dataset PostgreSQL uses.
 *
 * Mirrors PostgresBenchmarkTest in structure and methodology:
 *   - Same 8 queries, translated to OpenSearch idiom
 * Tagged "benchmark" — excluded from default test runs.
 */

@SpringBootTest
@ActiveProfiles({"benchmark", "stub"})
@Tag("benchmark")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenSearchBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchBenchmarkTest.class);

    @Autowired
    private OpenSearchProperties properties;

    private OpenSearchTransport transport;
    private OpenSearchClient client;
    private OpenSearchQueries queries;

    @BeforeAll
    void setup() {
        this.transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(properties.getScheme(), properties.getHost(), properties.getPort()))
                .build();
        this.client = new OpenSearchClient(transport);
        this.queries = new OpenSearchQueries(client, properties.getIndexName());
    }

    @AfterAll
    void tearDown() throws Exception {
        if (transport != null) transport.close();
    }

    @Test
    void runAllBenchmarks() throws Exception {
        BenchmarkResultWriter writer = new BenchmarkResultWriter(LocalDateTime.now());
        log.info("OpenSearch benchmark run. Output folder: {}", writer.getRunFolder());

        Map<String, Object> rowCounts = captureRowCounts();
        log.info("Row counts: {}", rowCounts);

        List<Map<String, Object>> summaries = new ArrayList<>();

        // Three queries get profile capture (headline queries).
        summaries.add(timedQuery(writer, 1, "Structured filter (OS)",
                queries::q1StructuredFilter, false));

        summaries.add(timedQuery(writer, 2, "Time range aggregation hourly (OS)",
                queries::q2HourlyAggregation, false));

        summaries.add(timedQuery(writer, 21, "Time range aggregation per-minute (OS)",
                queries::q21PerMinuteAggregation, true));  // PROFILED

        summaries.add(timedQuery(writer, 3, "High-cardinality groupby (OS)",
                queries::q3HighCardinalityGroupby, false));

        summaries.add(timedQuery(writer, 4, "Full-text search common term (OS)",
                queries::q4FullTextCommon, false));

        summaries.add(timedQuery(writer, 41, "Full-text search count all (OS)",
                queries::q41FullTextCountAll, true));  // PROFILED

        summaries.add(timedQuery(writer, 42, "Full-text search rare term (OS)",
                queries::q42FullTextRare, false));

        summaries.add(timedQuery(writer, 5, "Distinct cardinality (OS)",
                queries::q5DistinctCardinality, true));  // PROFILED

        writer.writeIndex(rowCounts, summaries);
        log.info("OpenSearch benchmark complete. Results in: {}", writer.getRunFolder());
    }

    /**
     * Runs a query 3 times, captures median. Optionally captures profile output.
     *
     * Profile is captured on a separate 4th run because including profile=true
     * adds overhead that would distort the timed runs.
     */
    private Map<String, Object> timedQuery(
            BenchmarkResultWriter writer,
            int queryNumber,
            String queryName,
            BenchmarkQuery queryFn,
            boolean withProfile) throws Exception {

        log.info("Q{}: {} — running 3 iterations", queryNumber, queryName);

        List<Long> timingsMs = new ArrayList<>();
        long resultCount = 0;

        for (int i = 0; i < 3; i++) {
            long startNanos = System.nanoTime();
            QueryResult result = queryFn.execute(false);
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            timingsMs.add(elapsedMs);
            resultCount = result.resultCount;
            log.debug("  run {}: {}ms, {} results", i + 1, elapsedMs, resultCount);
        }

        long medianMs = median(timingsMs);
        log.info("Q{}: {} — median {}ms ({} results)",
                queryNumber, queryName, medianMs, resultCount);

        // Optional profile capture on a separate run.
        String profileJson = null;
        if (withProfile) {
            log.debug("Capturing profile for Q{}", queryNumber);
            QueryResult profileResult = queryFn.execute(true);
            profileJson = profileResult.profileJson;
        }

        Map<String, Object> detailed = new LinkedHashMap<>();
        detailed.put("queryNumber", queryNumber);
        detailed.put("queryName", queryName);
        detailed.put("system", "OpenSearch");
        detailed.put("timingsMs", timingsMs);
        detailed.put("medianMs", medianMs);
        detailed.put("resultCount", resultCount);
        if (profileJson != null) {
            detailed.put("profile", profileJson);
        }

        // OpenSearch result files prefixed "os-" to distinguish from PG results.
        writer.writeQueryResult(queryNumber, "os-" + queryName.replaceAll("[^a-zA-Z0-9]", "-"), detailed);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("queryNumber", queryNumber);
        summary.put("queryName", queryName);
        summary.put("system", "OpenSearch");
        summary.put("medianMs", medianMs);
        summary.put("resultCount", resultCount);
        return summary;
    }

    private Map<String, Object> captureRowCounts() throws Exception {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("totalDocuments", client.count(req -> req.index(properties.getIndexName())).count());
        counts.put("indexName", properties.getIndexName());
        return counts;
    }

    private long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    /**
     * Functional interface for benchmark queries. Each query returns a result
     * containing the count of items returned and (optionally) profile JSON.
     */
    @FunctionalInterface
    interface BenchmarkQuery {
        QueryResult execute(boolean withProfile) throws Exception;
    }

    static class QueryResult {
        final long resultCount;
        final String profileJson;

        QueryResult(long resultCount, String profileJson) {
            this.resultCount = resultCount;
            this.profileJson = profileJson;
        }
    }
}