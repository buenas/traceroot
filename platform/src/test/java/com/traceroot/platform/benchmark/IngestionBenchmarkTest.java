package com.traceroot.platform.benchmark;

import com.traceroot.platform.common.Environment;
import com.traceroot.platform.common.Level;
import com.traceroot.platform.ingestion.LogEventRequest;
import com.traceroot.platform.ingestion.LogService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ingestion throughput benchmark.
 *
 * Measures how many logs per second TraceRoot's LogService can handle
 * through its real code path: validation, normalization, entity mapping,
 * JPA save. This number is separate from the query benchmarks — it
 * measures write throughput, not read performance.
 *
 * 10K logs is enough for a stable throughput measurement. 100K would
 * show the same per-log cost with more noise reduction, but takes
 * proportionally longer.
 *
 * What this exercises:
 *   - Bean Validation on LogEventRequest
 *   - LogNormalizer
 *   - LogRecordMapper
 *   - JpaRepository.save() per log
 *   - Incident detection on ERROR logs (this is real production behavior)
 *
 * Why detection stays enabled: we want to measure REAL ingestion throughput,
 * which includes the detection overhead. Turning it off would produce a
 * number that's not representative of what the real system achieves.
 *
 * Runs against traceroot_benchmark DB. Does NOT pollute the real DB.
 */
@SpringBootTest
@ActiveProfiles({"benchmark", "stub"})
@Tag("benchmark")
public class IngestionBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(IngestionBenchmarkTest.class);

    private static final int LOG_COUNT = 10_000;

    @Autowired
    private LogService logService;

    @Test
    void measureIngestionThroughput() {
        log.info("Ingestion benchmark: {} logs via LogService", LOG_COUNT);

        // Warm-up: 100 logs to initialize JIT, populate JPA caches.
        // Not timed — warm-up measurements are noisy and unfair.
        for (int i = 0; i < 100; i++) {
            logService.createLog(buildLogRequest(i));
        }
        log.info("Warm-up complete. Starting timed run.");

        long startMs = System.currentTimeMillis();

        for (int i = 0; i < LOG_COUNT; i++) {
            logService.createLog(buildLogRequest(i));

            if ((i + 1) % 1000 == 0) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                double rate = (i + 1) / (elapsedMs / 1000.0);
                log.info("  {} logs — {} logs/sec", i + 1, String.format("%.0f", rate));
            }
        }

        long totalMs = System.currentTimeMillis() - startMs;
        double throughput = LOG_COUNT / (totalMs / 1000.0);

        log.info("Ingestion benchmark complete.");
        log.info("  Total: {} logs in {}ms", LOG_COUNT, totalMs);
        log.info("  Throughput: {} logs/sec", String.format("%.0f", throughput));
        log.info("  Per-log latency: {}ms", String.format("%.2f", (double) totalMs / LOG_COUNT));
    }

    private LogEventRequest buildLogRequest(int index) {
        LogEventRequest request = new LogEventRequest();
        request.setLevel(index % 20 == 0 ? Level.ERROR : Level.INFO);
        request.setServiceName("benchmark-service");
        request.setMessage("Benchmark log #" + index);
        request.setEnvironment(Environment.PROD);
        request.setTimestamp(LocalDateTime.now());
        request.setTraceId(UUID.randomUUID().toString());
        request.setEndpoint("/benchmark/endpoint");
        request.setExceptionType(index % 20 == 0 ? "BenchmarkException" : null);
        request.setVersion("1.0.0");
        return request;
    }
}