package com.traceroot.failurelab.inventory;

public class ReserveInventoryRequest {
    private String traceId;
    private String orderId;
    private String sku;
    private int quantity;
    private boolean simulateNullPointer;
    private boolean simulateDependencyFailure;
    private boolean simulateStaleStock;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSimulateNullPointer() {
        return simulateNullPointer;
    }

    public void setSimulateNullPointer(boolean simulateNullPointer) {
        this.simulateNullPointer = simulateNullPointer;
    }

    public boolean isSimulateDependencyFailure() {
        return simulateDependencyFailure;
    }

    public void setSimulateDependencyFailure(boolean simulateDependencyFailure) {
        this.simulateDependencyFailure = simulateDependencyFailure;
    }

    public boolean isSimulateStaleStock() {
        return simulateStaleStock;
    }

    public void setSimulateStaleStock(boolean simulateStaleStock) {
        this.simulateStaleStock = simulateStaleStock;
    }
}