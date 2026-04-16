package com.traceroot.platform.metric;

public class ServiceIncidentMetricsResponse {

    private String serviceName;
    private Long incidentCount;
    private Long activeCount;
    private Long resolvedCount;


    public ServiceIncidentMetricsResponse(String serviceName, Long incidentCount, Long activeCount, Long resolvedCount) {
        this.serviceName = serviceName;
        this.incidentCount = incidentCount;
        this.activeCount = activeCount;
        this.resolvedCount = resolvedCount;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Long incidentCount) {
        this.incidentCount = incidentCount;
    }

    public Long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Long activeCount) {
        this.activeCount = activeCount;
    }

    public Long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(Long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
}
