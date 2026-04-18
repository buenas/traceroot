package com.traceroot.failurelab.inventory;

public class ReserveInventoryResponse {
    private boolean reserved;
    private String status;
    private String traceId;

    public ReserveInventoryResponse(boolean reserved, String status, String traceId) {
        this.reserved = reserved;
        this.status = status;
        this.traceId = traceId;
    }

    public boolean isReserved() {
        return reserved;
    }

    public String getStatus() {
        return status;
    }

    public String getTraceId() {
        return traceId;
    }
}