package com.traceroot.platform.benchmark;

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for OpenSearch local connection.
 *
 * Verifies three things:
 *   1. The Java client can connect to localhost:9200
 *   2. We can index a single document
 *   3. We can retrieve that document
 *
 * If all three pass, the foundation for bulk indexing and benchmarking
 * is solid.
 *
 * Tagged "benchmark" — excluded from default test runs since it requires
 * a running OpenSearch instance.
 *
 * Prerequisite:
 *   docker compose -f docker-compose.benchmark.yml up -d
 */
@Tag("benchmark")
public class OpenSearchSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchSmokeTest.class);

    private static final String TEST_INDEX = "smoke-test";
    private static final String TEST_DOC_ID = "smoke-1";

    @Test
    void canConnectIndexAndRetrieve() throws Exception {
        // Build the OpenSearch client. ApacheHttpClient5Transport is the
        // recommended modern transport — async, HTTP/2 capable.
        OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder
                        .builder(new HttpHost("http", "localhost", 9200))
                        .build();

        OpenSearchClient client = new OpenSearchClient(transport);

        try {
            // Test 1: Cluster health.
            // Confirms the connection works and we can read cluster state.
            HealthResponse health = client.cluster().health();
            log.info("Cluster status: {}", health.status());
            log.info("Number of nodes: {}", health.numberOfNodes());
            assertNotNull(health.status(), "Cluster health should be reachable");

            // Test 2: Index a single document.
            // We use a Map for the document body — the Java client serializes
            // it to JSON automatically. In real benchmark code we'll use
            // typed POJOs, but Map is fine for a smoke test.
            Map<String, Object> doc = new HashMap<>();
            doc.put("level", "INFO");
            doc.put("serviceName", "smoke-test-service");
            doc.put("message", "OpenSearch smoke test");
            doc.put("timestamp", "2026-04-29T18:00:00");

            IndexResponse indexResponse = client.index(req -> req
                            .index(TEST_INDEX)
                            .id(TEST_DOC_ID)
                            .document(doc)
                            .refresh(org.opensearch.client.opensearch._types.Refresh.True)
                    // refresh=true forces immediate visibility.
                    // In production we wouldn't do this on every write
                    // (it's expensive), but for a smoke test it ensures the
                    // document is searchable on the next line.
            );

            log.info("Indexed document. Result: {}", indexResponse.result());
            assertTrue(
                    indexResponse.result().jsonValue().equals("created") ||
                            indexResponse.result().jsonValue().equals("updated"),
                    "Document should be indexed successfully"
            );

            // Test 3: Retrieve the document.
            GetResponse<Map> getResponse = client.get(req -> req
                            .index(TEST_INDEX)
                            .id(TEST_DOC_ID),
                    Map.class
            );

            log.info("Retrieved document: found={}, source={}",
                    getResponse.found(), getResponse.source());
            assertTrue(getResponse.found(), "Document should be retrievable");
            assertEquals("smoke-test-service",
                    getResponse.source().get("serviceName"),
                    "Retrieved document should match indexed content");

            log.info("OpenSearch smoke test passed.");
        } finally {
            transport.close();
        }
    }
}