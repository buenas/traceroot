package com.traceroot.failurelab.order;

import com.traceroot.failurelab.common.Level;
import com.traceroot.failurelab.common.TraceRootLogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderService {

    public static final String SERVICE_NAME = "order-service";
    public static final String ENDPOINT = "/orders";

    // Retry storm parameters. Kept small so tests and demos don't block for long.
    private static final int RETRY_STORM_ATTEMPTS = 3;
    private static final long RETRY_STORM_DELAY_MS = 150;

    private final RestTemplate restTemplate;
    private final TraceRootLogClient traceRootLogClient;
    private final String inventoryUrl;
    private final String paymentUrl;

    public OrderService(TraceRootLogClient traceRootLogClient,
                        @Value("${clients.inventory.url}") String inventoryUrl,
                        @Value("${clients.payment.url}") String paymentUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restTemplate = new RestTemplate(factory);
        this.traceRootLogClient = traceRootLogClient;
        this.inventoryUrl = inventoryUrl;
        this.paymentUrl = paymentUrl;
    }

    /**
     * Order service orchestrates inventory and payment.
     * When the payment retry storm flag is set, the order service drives repeated
     * calls to the payment service to produce a realistic distributed failure pattern.
     */
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String traceId = request.getTraceId();

        reserveInventory(request, traceId);

        if (request.isSimulatePaymentRetryStorm()) {
            chargePaymentWithRetries(request, traceId);
        } else {
            chargePaymentOnce(request, traceId);
        }

        traceRootLogClient.sendLog(
                Level.INFO,
                SERVICE_NAME,
                "Order completed successfully",
                ENDPOINT,
                null,
                traceId
        );

        return new CreateOrderResponse(true, "ORDER_CREATED", traceId);
    }

    private void reserveInventory(CreateOrderRequest request, String traceId) {
        try {
            InventoryReserveClientRequest inventoryRequest = new InventoryReserveClientRequest();
            inventoryRequest.setTraceId(traceId);
            inventoryRequest.setOrderId(request.getOrderId());
            inventoryRequest.setSku(request.getSku());
            inventoryRequest.setQuantity(request.getQuantity());
            inventoryRequest.setSimulateNullPointer(request.isSimulateInventoryNullPointer());
            inventoryRequest.setSimulateDependencyFailure(request.isSimulateInventoryDependencyFailure());
            inventoryRequest.setSimulateStaleStock(false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(
                    inventoryUrl + "/inventory/reserve",
                    new HttpEntity<>(inventoryRequest, headers),
                    String.class
            );
        } catch (Exception ex) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Inventory dependency failed during order creation",
                    ENDPOINT,
                    "InventoryCallFailedException",
                    traceId
            );
            throw new RuntimeException("InventoryCallFailedException", ex);
        }
    }

    private void chargePaymentOnce(CreateOrderRequest request, String traceId) {
        try {
            PaymentChargeClientRequest paymentRequest = buildPaymentRequest(request, traceId, false);
            callPayment(paymentRequest);
        } catch (Exception ex) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Payment dependency failed during order creation",
                    ENDPOINT,
                    "PaymentCallFailedException",
                    traceId
            );
            throw new RuntimeException("PaymentCallFailedException", ex);
        }
    }

    /**
     * Drives a realistic retry storm against the payment service.
     * Each attempt is a separate HTTP call, so each attempt produces its own log event
     * in payment-service, spread across time. After all attempts fail, the order service
     * logs its own failure and throws.
     */
    private void chargePaymentWithRetries(CreateOrderRequest request, String traceId) {
        PaymentChargeClientRequest paymentRequest = buildPaymentRequest(request, traceId, true);

        Exception lastException = null;

        for (int attempt = 1; attempt <= RETRY_STORM_ATTEMPTS; attempt++) {
            try {
                callPayment(paymentRequest);
                // Success shouldn't happen when transient-failure is set,
                // but if it does, exit cleanly.
                return;
            } catch (Exception ex) {
                lastException = ex;
                traceRootLogClient.sendLog(
                        Level.WARN,
                        SERVICE_NAME,
                        "Payment call failed on attempt " + attempt + " of " + RETRY_STORM_ATTEMPTS,
                        ENDPOINT,
                        "PaymentRetryAttemptException",
                        traceId
                );

                if (attempt < RETRY_STORM_ATTEMPTS) {
                    sleepQuietly(RETRY_STORM_DELAY_MS);
                }
            }
        }

        // All retries exhausted. Log the final failure and propagate.
        traceRootLogClient.sendLog(
                Level.ERROR,
                SERVICE_NAME,
                "Payment retry storm exhausted after " + RETRY_STORM_ATTEMPTS + " attempts",
                ENDPOINT,
                "PaymentRetryStormException",
                traceId
        );
        throw new RuntimeException("PaymentRetryStormException", lastException);
    }

    private PaymentChargeClientRequest buildPaymentRequest(CreateOrderRequest request,
                                                           String traceId,
                                                           boolean retryStormMode) {
        PaymentChargeClientRequest paymentRequest = new PaymentChargeClientRequest();
        paymentRequest.setTraceId(traceId);
        paymentRequest.setOrderId(request.getOrderId());
        paymentRequest.setAmount(request.getAmount());
        paymentRequest.setSimulateTimeout(request.isSimulatePaymentTimeout());
        paymentRequest.setSimulateProviderDown(request.isSimulatePaymentProviderDown());
        // In retry storm mode, every attempt fails transiently.
        // Otherwise, the transient-failure flag is driven by nothing and stays false.
        paymentRequest.setSimulateTransientFailure(retryStormMode);
        return paymentRequest;
    }

    private void callPayment(PaymentChargeClientRequest paymentRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(
                paymentUrl + "/payments/charge",
                new HttpEntity<>(paymentRequest, headers),
                String.class
        );
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}