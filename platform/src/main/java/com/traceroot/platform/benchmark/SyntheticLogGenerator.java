package com.traceroot.platform.benchmark;

import com.traceroot.platform.common.Environment;
import com.traceroot.platform.common.Level;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Generates synthetic log entries with mostly-realistic distributions
 * for benchmark seeding.
 * - Log level ratios match typical production: 80% INFO, 15% WARN, 5% ERROR.
 * Instantiated once by LogSeedRunner. Not a Spring bean — pure utility.
 */
public class SyntheticLogGenerator {

    // Fixed sets. 5 services, matching realistic microservice footprint.
    private static final List<String> SERVICES = List.of(
            "api-gateway",
            "order-service",
            "inventory-service",
            "payment-service",
            "notification-service"
    );

    // 20 endpoints total, distributed across services.
    // Real systems have hot endpoints + long tail; we approximate with weighted selection below.
    private static final List<String> ENDPOINTS = List.of(
            "/checkout",
            "/orders",
            "/orders/{id}",
            "/orders/cancel",
            "/inventory/reserve",
            "/inventory/release",
            "/inventory/check",
            "/payments/charge",
            "/payments/refund",
            "/payments/status",
            "/notifications/send",
            "/notifications/retry",
            "/health",
            "/metrics",
            "/users/{id}",
            "/users/auth",
            "/cart/add",
            "/cart/remove",
            "/cart/checkout",
            "/admin/debug"
    );

    // 10 exception types. Concentrated on realistic backend failures.
    private static final List<String> EXCEPTION_TYPES = List.of(
            "TimeoutException",
            "NullPointerException",
            "ConnectionRefusedException",
            "ValidationException",
            "InventoryDependencyException",
            "ProviderUnavailableException",
            "DatabaseTimeoutException",
            "AuthenticationException",
            "RateLimitException",
            "TransientPaymentException"
    );

    // Message templates. Format specifiers filled per-log.
    // Mix of short and long messages, with some containing common search
    // terms ("timeout", "connection", "retry") so full-text search has
    // realistic match rates.
    private static final List<String> MESSAGE_TEMPLATES = List.of(
            "Successfully processed request",
            "Request completed in %dms",
            "Retrying operation after failure",
            "Connection refused to downstream service",
            "Timeout while waiting for response",
            "Null pointer encountered during operation",
            "Invalid input received",
            "Rate limit exceeded for client",
            "Database query took %dms",
            "Downstream service returned error",
            "Request validated successfully",
            "Cache miss, fetching from source",
            "Unable to reach payment provider",
            "Retry attempt %d of 3 failed",
            "Operation completed successfully"
    );

    private final Random random;

    public SyntheticLogGenerator(long seed) {
        // Seeded for reproducibility. Same seed, same output across runs.
        this.random = new Random(seed);
    }

    /**
     * Generates a single synthetic log record as a tuple of field values
     * ready for direct SQL INSERT. Order matches the column order in
     * LogSeedRunner's INSERT statement.
     * Returning Object[] instead of a DTO to avoid mapping overhead at
     * 10M-row scale. Every microsecond matters here.
     */
    public Object[] generateLog() {
        UUID id = UUID.randomUUID();
        LocalDateTime timestamp = randomTimestamp();
        Level level = randomLevel();
        String service = randomService();
        String endpoint = randomEndpoint();
        String exceptionType = level == Level.ERROR ? randomExceptionType() : null;
        String message = randomMessage();
        String traceId = UUID.randomUUID().toString();
        Environment environment = Environment.PROD;
        String version = "1.0." + random.nextInt(10);

        // Column order: id, created_at, endpoint, environment, exception_type,
        // level, message, service_name, timestamp, trace_id, version
        return new Object[]{
                id,
                LocalDateTime.now(),    // created_at: when this row was inserted
                endpoint,
                environment.name(),
                exceptionType,
                level.name(),
                message,
                service,
                timestamp,              // timestamp: synthetic, spread across 30 days
                traceId,
                version
        };
    }

    /**
     * Random timestamp in the last 30 days.
     * Slight recency weighting: uniform random is fine for our purposes
     * but we skew the last 24 hours to get ~20% of data there (roughly
     * matching what a real production system's "recent" queries would hit).
     */
    private LocalDateTime randomTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        int roll = random.nextInt(100);
        long secondsAgo;
        if (roll < 20) {
            // 20% in the last 24 hours
            secondsAgo = random.nextInt(86_400);
        } else if (roll < 60) {
            // 40% in the last 7 days
            secondsAgo = random.nextInt(86_400 * 7);
        } else {
            // 40% across the full 30 days
            secondsAgo = random.nextInt(86_400 * 30);
        }
        return now.minusSeconds(secondsAgo);
    }

    /**
     * 80% INFO, 15% WARN, 5% ERROR.
     * Matches typical production distribution. ERROR rate matters because
     * our structured-filter benchmark (Query 1) filters by level=ERROR.
     */
    private Level randomLevel() {
        int roll = random.nextInt(100);
        if (roll < 80) return Level.INFO;
        if (roll < 95) return Level.WARN;
        return Level.ERROR;
    }

    /**
     * Weighted service selection: api-gateway 40%, order-service 25%,
     * others split the remaining 35%. Not Zipfian, but captures the
     * "hot service" characteristic.
     */
    private String randomService() {
        int roll = random.nextInt(100);
        if (roll < 40) return "api-gateway";
        if (roll < 65) return "order-service";
        if (roll < 80) return "inventory-service";
        if (roll < 92) return "payment-service";
        return "notification-service";
    }

    private String randomEndpoint() {
        return ENDPOINTS.get(random.nextInt(ENDPOINTS.size()));
    }

    private String randomExceptionType() {
        return EXCEPTION_TYPES.get(random.nextInt(EXCEPTION_TYPES.size()));
    }

    private String randomMessage() {
        String template = MESSAGE_TEMPLATES.get(random.nextInt(MESSAGE_TEMPLATES.size()));
        if (template.contains("%d")) {
            return String.format(template, random.nextInt(5000));
        }
        return template;
    }
}