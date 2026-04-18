package com.traceroot.failurelab.gateway;

public class CheckoutResponse {
    private boolean success;
    private String status;
    private String traceId;

    public CheckoutResponse(boolean success, String status, String traceId) {
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
