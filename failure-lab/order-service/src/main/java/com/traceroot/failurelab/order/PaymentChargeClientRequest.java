package com.traceroot.failurelab.order;

public class PaymentChargeClientRequest {
    private String traceId;
    private String orderId;
    private Double amount;
    private boolean simulateTimeout;
    private boolean simulateProviderDown;
    private boolean simulateTransientFailure;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public boolean isSimulateTimeout() { return simulateTimeout; }
    public void setSimulateTimeout(boolean simulateTimeout) { this.simulateTimeout = simulateTimeout; }
    public boolean isSimulateProviderDown() { return simulateProviderDown; }
    public void setSimulateProviderDown(boolean simulateProviderDown) { this.simulateProviderDown = simulateProviderDown; }
    public boolean isSimulateTransientFailure() { return simulateTransientFailure; }
    public void setSimulateTransientFailure(boolean simulateTransientFailure) { this.simulateTransientFailure = simulateTransientFailure; }
}