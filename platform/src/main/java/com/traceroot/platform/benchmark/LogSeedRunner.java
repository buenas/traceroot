package com.traceroot.platform.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the traceroot_benchmark database with synthetic logs.
 *
 * Activated by the "seed" profile. Runs once as a CommandLineRunner and
 * the app stays up — you Ctrl+C when seeding finishes.
 *
 * To run:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=benchmark,seed
 *
 * Configuration:
 *   - target row count via traceroot.benchmark.seed.count
 *   - batch size via traceroot.benchmark.seed.batch-size
 *
 * Design choices:
 *
 * - Direct SQL via JdbcTemplate.batchUpdate. At 10M rows, anything else
 *   (JPA save, entity manager, even row-by-row JDBC) is 50-100x slower.
 *   The goal here is throughput, not correctness of the service layer.
 *
 * - Batch size of 5,000. Larger batches use more memory per call, smaller
 *   batches waste round-trips. 5K is a reasonable middle on typical laptop
 *   hardware. Tunable via properties if needed.
 *
 * - Progress logging every 100K rows. Enough to see it's alive, not so
 *   much that it floods the console.
 *
 * - Fixed seed (42) for reproducibility. Same synthetic data every run.
 *
 * What this deliberately does NOT do:
 *   - Does not run incident detection
 *   - Does not go through LogService or any HTTP layer
 *   - Does not create indexes (see createIndexes() note below)
 *   - Does not touch the real traceroot database
 */
@Component
@Profile({"benchmark", "seed"})  // Both profiles required: benchmark config + seed trigger
public class LogSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LogSeedRunner.class);

    private static final String INSERT_SQL = """
        INSERT INTO logs
            (id, created_at, endpoint, environment, exception_type,
             level, message, service_name, timestamp, trace_id, version)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private final JdbcTemplate jdbcTemplate;

    @Value("${traceroot.benchmark.seed.count:10000000}")
    private int targetCount;

    @Value("${traceroot.benchmark.seed.batch-size:5000}")
    private int batchSize;

    @Value("${traceroot.benchmark.seed.random-seed:42}")
    private long randomSeed;

    public LogSeedRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("Starting seed: target={} logs, batchSize={}", targetCount, batchSize);
        long startMs = System.currentTimeMillis();

        // Safety check: refuse to run if logs table is not empty.
        // Protects against accidentally re-seeding and ballooning the DB.
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs", Integer.class);
        if (existingCount != null && existingCount > 0) {
            log.warn("logs table already has {} rows. Skipping seed. " +
                    "Truncate manually if you want to re-seed.", existingCount);
            return;
        }

        SyntheticLogGenerator generator = new SyntheticLogGenerator(randomSeed);

        int totalInserted = 0;
        List<Object[]> batch = new ArrayList<>(batchSize);

        while (totalInserted < targetCount) {
            batch.clear();
            int thisBatchSize = Math.min(batchSize, targetCount - totalInserted);

            for (int i = 0; i < thisBatchSize; i++) {
                batch.add(generator.generateLog());
            }

            jdbcTemplate.batchUpdate(INSERT_SQL, batch);
            totalInserted += thisBatchSize;

            if (totalInserted % 100_000 == 0 || totalInserted == targetCount) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                double rowsPerSec = (totalInserted / (elapsedMs / 1000.0));
                log.info("Inserted {} logs ({}%) — {} rows/sec",
                        totalInserted,
                        (totalInserted * 100 / targetCount),
                        String.format("%,.0f", rowsPerSec));
            }
        }

        long totalMs = System.currentTimeMillis() - startMs;
        log.info("Seed complete. {} logs inserted in {}s ({} rows/sec).",
                totalInserted,
                totalMs / 1000,
                String.format("%,.0f", totalInserted / (totalMs / 1000.0)));

        // Intentionally not creating indexes here.
        // Query benchmarks include index analysis as part of the article.
        // If we pre-build indexes, we hide the "PostgreSQL is slow without
        // the right index" story that readers want to see.
        //
        // To add indexes manually after seeding:
        //   CREATE INDEX idx_logs_service ON logs (service_name);
        //   CREATE INDEX idx_logs_level ON logs (level);
        //   CREATE INDEX idx_logs_timestamp ON logs (timestamp);
        //
        // Run benchmarks both with and without indexes to show the effect.

        log.info("To add indexes for comparison, run manually in psql:");
        log.info("  CREATE INDEX idx_logs_service ON logs (service_name);");
        log.info("  CREATE INDEX idx_logs_level ON logs (level);");
        log.info("  CREATE INDEX idx_logs_timestamp ON logs (timestamp);");
    }
}