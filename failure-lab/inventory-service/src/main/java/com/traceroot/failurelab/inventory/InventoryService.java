package com.traceroot.failurelab.inventory;

import com.traceroot.failurelab.common.Level;
import com.traceroot.failurelab.common.TraceRootLogClient;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    public static final String SERVICE_NAME = "inventory-service";
    public static final String ENDPOINT = "/inventory/reserve";

    private final TraceRootLogClient traceRootLogClient;

    public InventoryService(TraceRootLogClient traceRootLogClient) {
        this.traceRootLogClient = traceRootLogClient;
    }

    /**
     * Inventory service simulates non-timeout operational failures so TraceRoot
     * can distinguish inventory-related incidents from payment-related ones.
     */
    public ReserveInventoryResponse reserve(ReserveInventoryRequest request) {
        String traceId = request.getTraceId();

        if (request.isSimulateNullPointer()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Null pointer while reserving stock",
                    ENDPOINT,
                    "NullPointerException",
                    traceId
            );
            throw new RuntimeException("NullPointerException");
        }

        if (request.isSimulateDependencyFailure()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Inventory dependency unavailable",
                    ENDPOINT,
                    "InventoryDependencyException",
                    traceId
            );
            throw new RuntimeException("InventoryDependencyException");
        }

        if (request.isSimulateStaleStock()) {
            traceRootLogClient.sendLog(
                    Level.ERROR,
                    SERVICE_NAME,
                    "Stale stock state detected during reservation",
                    ENDPOINT,
                    "StaleStockException",
                    traceId
            );
            throw new RuntimeException("StaleStockException");
        }

        traceRootLogClient.sendLog(
                Level.INFO,
                SERVICE_NAME,
                "Inventory reserved successfully",
                ENDPOINT,
                null,
                traceId
        );

        return new ReserveInventoryResponse(true, "RESERVED", traceId);
    }
}