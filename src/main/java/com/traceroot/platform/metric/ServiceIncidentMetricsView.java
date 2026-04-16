package com.traceroot.platform.metric;

public interface ServiceIncidentMetricsView {
    String getServiceName();
    Long getIncidentCount();
    Long getActiveCount();
    Long getResolvedCount();
}