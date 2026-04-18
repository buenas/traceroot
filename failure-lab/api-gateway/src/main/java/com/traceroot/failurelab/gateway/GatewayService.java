package com.traceroot.failurelab.gateway;
import com.traceroot.failurelab.common.Level;
import com.traceroot.failurelab.common.TraceRootLogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Service
public class GatewayService {

    public static final String SERVICE_NAME = "api-gateway";
    public static final String ENDPOINT = "/checkout";

    private final RestTemplate restTemplate;
    private final TraceRootLogClient traceRootLogClient;
    private final String orderServiceUrl;

    public GatewayService(RestTemplateBuilder builder,
                                     TraceRootLogClient traceRootLogClient,
                                     @Value("${clients.order.url}") String orderServiceUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.traceRootLogClient = traceRootLogClient;
        this.orderServiceUrl = orderServiceUrl;
    }

    /**
     * API gateway is the entry point into the Failure Lab.
     * It is useful for testing top-level propagation of downstream failures.
     */
    public CheckoutResponse checkout(CheckoutRequest request) {
        try {
            GatewayOrderRequest orderRequest = new GatewayOrderRequest();
            orderRequest.setTraceId(request.getTraceId());
            orderRequest.setOrderId(request.getOrderId());
            orderRequest.setSku(request.getSku());
            orderRequest.setQuantity(request.getQuantity());
            orderRequest.setAmount(request.getAmount());
            orderRequest.setSimulateInventoryNullPointer(request.isSimulateInventoryNullPointer());
            orderRequest.setSimulateInventoryDependencyFailure(request.isSimulateInventoryDependencyFailure());
            orderRequest.setSimulatePaymentTimeout(request.isSimulatePaymentTimeout());
            orderRequest.setSimulatePaymentProviderDown(request.isSimulatePaymentProviderDown());
            orderRequest.setSimulatePaymentBurstErrors(request.isSimulatePaymentRetryStorm());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(orderServiceUrl + "/orders", new HttpEntity<>(orderRequest, headers), String.class);

            traceRootLogClient.sendLog(
                    Level.INFO,
                    SERVICE_NAME,
                    "Checkout completed successfully",
                    ENDPOINT,
                    null,
                    request.getTraceId()
            );

            return new CheckoutResponse(true, "CHECKOUT_COMPLETED", request.getTraceId());
        } catch (Exception ex) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Gateway observed downstream failure during checkout",
                    ENDPOINT,
                    "GatewayDownstreamException",
                    request.getTraceId()
            );
            throw new RuntimeException("GatewayDownstreamException", ex);
        }
    }
}