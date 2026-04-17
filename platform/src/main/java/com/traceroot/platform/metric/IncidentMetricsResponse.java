package com.traceroot.platform.metric;

public class IncidentMetricsResponse {
    private long activeIncidents;
    private long resolvedIncidents;
    private long totalIncidents;
    private long incidentsCreatedLast24Hours;
    private long incidentsResolvedLast24Hours;
    private Double averageResolutionMinutes;

    public IncidentMetricsResponse(long activeIncidents, long resolvedIncidents, long totalIncidents, long incidentsCreatedLast24Hours, long incidentsResolvedLast24Hours, Double averageResolutionMinutes) {
        this.activeIncidents = activeIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.totalIncidents = totalIncidents;
        this.incidentsCreatedLast24Hours = incidentsCreatedLast24Hours;
        this.incidentsResolvedLast24Hours = incidentsResolvedLast24Hours;
        this.averageResolutionMinutes = averageResolutionMinutes;
    }

    public long getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(long activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    public long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public void setResolvedIncidents(long resolvedIncidents) {
        this.resolvedIncidents = resolvedIncidents;
    }

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getIncidentsCreatedLast24Hours() {
        return incidentsCreatedLast24Hours;
    }

    public void setIncidentsCreatedLast24Hours(long incidentsCreatedLast24Hours) {
        this.incidentsCreatedLast24Hours = incidentsCreatedLast24Hours;
    }

    public long getIncidentsResolvedLast24Hours() {
        return incidentsResolvedLast24Hours;
    }

    public void setIncidentsResolvedLast24Hours(long incidentsResolvedLast24Hours) {
        this.incidentsResolvedLast24Hours = incidentsResolvedLast24Hours;
    }

    public Double getAverageResolutionMinutes() {
        return averageResolutionMinutes;
    }

    public void setAverageResolutionMinutes(Double averageResolutionMinutes) {
        this.averageResolutionMinutes = averageResolutionMinutes;
    }
}
