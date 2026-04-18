package com.traceroot.failurelab.gateway;

public class GatewayOrderRequest {
    private String traceId;
    private String orderId;
    private String sku;
    private int quantity;
    private double amount;
    private boolean simulateInventoryNullPointer;
    private boolean simulateInventoryDependencyFailure;
    private boolean simulatePaymentTimeout;
    private boolean simulatePaymentProviderDown;
    private boolean simulatePaymentBurstErrors;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public boolean isSimulateInventoryNullPointer() { return simulateInventoryNullPointer; }
    public void setSimulateInventoryNullPointer(boolean simulateInventoryNullPointer) { this.simulateInventoryNullPointer = simulateInventoryNullPointer; }
    public boolean isSimulateInventoryDependencyFailure() { return simulateInventoryDependencyFailure; }
    public void setSimulateInventoryDependencyFailure(boolean simulateInventoryDependencyFailure) { this.simulateInventoryDependencyFailure = simulateInventoryDependencyFailure; }
    public boolean isSimulatePaymentTimeout() { return simulatePaymentTimeout; }
    public void setSimulatePaymentTimeout(boolean simulatePaymentTimeout) { this.simulatePaymentTimeout = simulatePaymentTimeout; }
    public boolean isSimulatePaymentProviderDown() { return simulatePaymentProviderDown; }
    public void setSimulatePaymentProviderDown(boolean simulatePaymentProviderDown) { this.simulatePaymentProviderDown = simulatePaymentProviderDown; }
    public boolean isSimulatePaymentBurstErrors() { return simulatePaymentBurstErrors; }
    public void setSimulatePaymentBurstErrors(boolean simulatePaymentBurstErrors) { this.simulatePaymentBurstErrors = simulatePaymentBurstErrors; }
}
