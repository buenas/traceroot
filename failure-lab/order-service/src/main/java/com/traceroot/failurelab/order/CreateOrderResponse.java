package com.traceroot.failurelab.order;

public class CreateOrderResponse {
    private boolean success;
    private String status;
    private String traceId;

    public CreateOrderResponse(boolean success, String status, String traceId) {
        this.success = success;
        this.status = status;
        this.traceId = traceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public String getTraceId() {
        return traceId;
    }
}