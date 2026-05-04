package com.traceroot.platform.benchmark;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.DateProperty;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bulk-loads logs from PostgreSQL into OpenSearch for benchmark comparison.
 *
 * Reads from traceroot_benchmark.logs table.
 * Writes to OpenSearch index "logs" via the bulk API.
 * Activated by profiles "benchmark,opensearch-load".
 *
 * Idempotency: if the OpenSearch index already exists with documents,
 * the loader logs a warning and skips. To re-load, delete the index first:curl -X DELETE http://localhost:9200/logs
 *
 * Performance optimizations applied during bulk loading:
 *   - 0 replicas (no replication overhead)
 *   - refresh_interval=-1 (disable searchable refresh during writes)
 *   - bulk batches of 5,000 documents
 *   - explicit refresh after all data is loaded
 *
 * Expected runtime: ~30-60 minutes for 10M documents on developer hardware.
 */

@Component
@Profile({"benchmark", "opensearch-load"})
public class OpenSearchBulkLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchBulkLoader.class);

    private static final String SELECT_SQL = """
        SELECT id, created_at, endpoint, environment, exception_type,
               level, message, service_name, timestamp, trace_id, version
        FROM logs
        """;

    private final JdbcTemplate jdbcTemplate;
    private final OpenSearchProperties properties;

    public OpenSearchBulkLoader(JdbcTemplate jdbcTemplate, OpenSearchProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting OpenSearch bulk load");
        long startMs = System.currentTimeMillis();

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(
                        properties.getScheme(),
                        properties.getHost(),
                        properties.getPort()))
                .build();

        OpenSearchClient client = new OpenSearchClient(transport);

        try {
            // Step 1: Check if index already has data. If so, skip.
            if (indexExistsWithData(client)) {
                log.warn("Index '{}' already exists with documents. Skipping load. " +
                                "Delete the index manually if you want to re-load: " +
                                "curl -X DELETE http://localhost:9200/{}",
                        properties.getIndexName(), properties.getIndexName());
                return;
            }

            // Step 2: Create the index with explicit mapping and bulk-friendly settings.
            createIndexWithMapping(client);

            // Step 3: Stream documents from PostgreSQL and bulk-load to OpenSearch.
            int totalLoaded = streamFromPostgresAndIndex(client);

            // Step 4: Restore normal refresh settings and trigger a final refresh.
            finalizeIndex(client);

            long totalSec = (System.currentTimeMillis() - startMs) / 1000;
            log.info("Bulk load complete. {} documents indexed in {}s ({} docs/sec)",
                    totalLoaded, totalSec,
                    String.format("%,.0f", (double) totalLoaded / Math.max(1, totalSec)));
        } finally {
            transport.close();
        }
    }

    /**
     * Returns true if the index exists and contains at least one document.
     * Used for idempotency — if the loader has already run, we skip.
     */
    private boolean indexExistsWithData(OpenSearchClient client) {
        try {
            boolean exists = client.indices().exists(req ->
                    req.index(properties.getIndexName())
            ).value();
            if (!exists) return false;

            long count = client.count(req ->
                    req.index(properties.getIndexName())
            ).count();

            return count > 0;
        } catch (Exception e) {
            // If we can't check, err on the side of letting the loader proceed.
            // Worst case: we get an "index already exists" error from create.
            log.warn("Could not check index existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates the OpenSearch index with explicit field mappings.
     *
     * Most fields use keyword type for fast aggregations and exact match.
     * Only the message field uses text type for full-text search.
     *
     * Sets index settings for bulk-load performance:
     *   - 0 replicas (no replication during load)
     *   - refresh_interval=-1 (disable searchable refresh)
     */
    private void createIndexWithMapping(OpenSearchClient client) throws Exception {
        log.info("Creating index '{}' with explicit mapping", properties.getIndexName());

        // Build the field mapping. Most fields are keyword for exact-match
        // aggregation. Only message is text (tokenized for full-text search).
        Map<String, Property> properties = new HashMap<>();

        properties.put("id", keywordProperty());
        properties.put("level", keywordProperty());
        properties.put("service_name", keywordProperty());
        properties.put("environment", keywordProperty());
        properties.put("trace_id", keywordProperty());
        properties.put("endpoint", keywordProperty());
        properties.put("exception_type", keywordProperty());
        properties.put("version", keywordProperty());
        properties.put("message", textProperty());
        properties.put("timestamp", dateProperty());
        properties.put("created_at", dateProperty());

        TypeMapping mapping = TypeMapping.of(m -> m.properties(properties));

        // Bulk-load performance settings.
        IndexSettings settings = IndexSettings.of(s -> s
                .numberOfReplicas("0")
                .refreshInterval(t -> t.time("-1"))
        );

        CreateIndexRequest createRequest = CreateIndexRequest.of(req -> req
                .index(this.properties.getIndexName())
                .mappings(mapping)
                .settings(settings)
        );

        client.indices().create(createRequest);
        log.info("Index created");
    }

    private Property keywordProperty() {
        return Property.of(p -> p.keyword(KeywordProperty.of(k -> k)));
    }

    private Property textProperty() {
        return Property.of(p -> p.text(TextProperty.of(t -> t)));
    }

    private Property dateProperty() {
        // strict_date_optional_time matches our LocalDateTime serialization format.
        return Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time"))));
    }

    /**
     * Streams documents from PostgreSQL using setFetchSize for memory efficiency,
     * accumulates them into bulk-sized batches, and indexes each batch.
     *
     * Why streaming: a 10M-row ResultSet would otherwise load entirely into
     * memory. Setting fetch size makes the JDBC driver pull rows in chunks.
     */
    private int streamFromPostgresAndIndex(OpenSearchClient client) throws Exception {
        log.info("Streaming from PostgreSQL and bulk-indexing to OpenSearch...");

        List<LogDocument> batch = new ArrayList<>(properties.getBulkBatchSize());
        int[] totalLoaded = {0}; // mutable holder for use inside lambda

        jdbcTemplate.setFetchSize(properties.getPgFetchBatchSize());
        jdbcTemplate.query(SELECT_SQL, (RowCallbackHandler) rs -> {
            try {
                LogDocument doc = mapRowToDocument(rs);
                batch.add(doc);

                if (batch.size() >= properties.getBulkBatchSize()) {
                    bulkIndex(client, batch);
                    totalLoaded[0] += batch.size();
                    batch.clear();

                    if (totalLoaded[0] % 100_000 == 0) {
                        log.info("Indexed {} documents ({}%)",
                                totalLoaded[0],
                                (totalLoaded[0] * 100L) / 10_000_000L);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to bulk index batch", e);
            }
        });

        // Index any remaining documents in the final partial batch.
        if (!batch.isEmpty()) {
            bulkIndex(client, batch);
            totalLoaded[0] += batch.size();
        }

        return totalLoaded[0];
    }

    /**
     * Maps a JDBC ResultSet row to a LogDocument.
     * Field-by-field assignment to keep the data path explicit.
     */
    private LogDocument mapRowToDocument(ResultSet rs) throws SQLException {
        LogDocument doc = new LogDocument();
        doc.setId(UUID.fromString(rs.getString("id")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        doc.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        doc.setEndpoint(rs.getString("endpoint"));
        doc.setEnvironment(rs.getString("environment"));
        doc.setExceptionType(rs.getString("exception_type"));
        doc.setLevel(rs.getString("level"));
        doc.setMessage(rs.getString("message"));
        doc.setServiceName(rs.getString("service_name"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        doc.setTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);

        doc.setTraceId(rs.getString("trace_id"));
        doc.setVersion(rs.getString("version"));
        return doc;
    }

    /**
     * Sends one bulk request containing all documents in the batch.
     * OpenSearch's bulk API accepts a list of operations in one HTTP call.
     */
    private void bulkIndex(OpenSearchClient client, List<LogDocument> batch) throws Exception {
        List<BulkOperation> operations = new ArrayList<>(batch.size());
        for (LogDocument doc : batch) {
            BulkOperation op = BulkOperation.of(b -> b.index(
                    IndexOperation.of(i -> i
                            .index(properties.getIndexName())
                            .id(doc.getId().toString())
                            .document(doc))
            ));
            operations.add(op);
        }

        BulkRequest bulkRequest = BulkRequest.of(req -> req.operations(operations));
        BulkResponse response = client.bulk(bulkRequest);

        if (response.errors()) {
            // Log first failure for diagnostic purposes; don't abort the whole load
            // for partial failures (which can happen on individual document validation).
            response.items().stream()
                    .filter(item -> item.error() != null)
                    .findFirst()
                    .ifPresent(item -> log.warn("Bulk operation had errors. First error: {}",
                            item.error().reason()));
        }
    }

    /**
     * Restores normal refresh interval and triggers a final refresh.
     * After this call, all indexed documents are searchable.
     */
    private void finalizeIndex(OpenSearchClient client) throws Exception {
        log.info("Finalizing index: restoring refresh interval and triggering refresh");

        client.indices().putSettings(req -> req
                .index(properties.getIndexName())
                .settings(s -> s.refreshInterval(t -> t.time("1s")))
        );

        client.indices().refresh(req -> req.index(properties.getIndexName()));

        long count = client.count(req -> req.index(properties.getIndexName())).count();
        log.info("Index '{}' now contains {} searchable documents",
                properties.getIndexName(), count);
    }
}