package com.traceroot.failurelab.payment;

import com.traceroot.failurelab.common.Level;
import com.traceroot.failurelab.common.TraceRootLogClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    public static final String SERVICE_NAME = "payment-service";
    public static final String ENDPOINT = "/payments/charge";

    private final TraceRootLogClient traceRootLogClient;

    public PaymentService(TraceRootLogClient traceRootLogClient) {
        this.traceRootLogClient = traceRootLogClient;
    }

    /**
     * Payment service emits a single log per request and throws on simulated failures.
     * The order service is responsible for retrying transient failures, which is how
     * real retry storms are produced in distributed systems.
     */
    public ChargeResponse charge(ChargeRequest request) {
        String traceId = request.getTraceId();

        if (request.isSimulateTransientFailure()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Transient payment failure; caller may retry",
                    ENDPOINT,
                    "TransientPaymentException",
                    traceId
            );
            throw new RuntimeException("TransientPaymentException");
        }

        if (request.isSimulateTimeout()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Timeout while charging downstream provider",
                    ENDPOINT,
                    "TimeoutException",
                    traceId
            );
            throw new RuntimeException("TimeoutException");
        }

        if (request.isSimulateProviderDown()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Downstream payment provider unavailable",
                    ENDPOINT,
                    "ProviderUnavailableException",
                    traceId
            );
            throw new RuntimeException("ProviderUnavailableException");
        }

        traceRootLogClient.sendLog(
                Level.INFO,
                SERVICE_NAME,
                "Payment charged successfully",
                ENDPOINT,
                null,
                traceId
        );

        return new ChargeResponse(true, "CHARGED", traceId);
    }
}