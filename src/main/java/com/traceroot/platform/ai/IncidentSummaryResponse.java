package com.traceroot.platform.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class IncidentSummaryResponse {
    private UUID incidentId;
    private String summary;
    private String possibleCause;
    private List<String> recommendedChecks;
    private LocalDateTime generatedAt;

    public IncidentSummaryResponse(UUID incidentId,
                                   String summary,
                                   String possibleCause,
                                   List<String> recommendedChecks,
                                   LocalDateTime generatedAt) {
        this.incidentId = incidentId;
        this.summary = summary;
        this.possibleCause = possibleCause;
        this.recommendedChecks = recommendedChecks;
        this.generatedAt = generatedAt;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPossibleCause() {
        return possibleCause;
    }

    public void setPossibleCause(String possibleCause) {
        this.possibleCause = possibleCause;
    }

    public List<String> getRecommendedChecks() {
        return recommendedChecks;
    }

    public void setRecommendedChecks(List<String> recommendedChecks) {
        this.recommendedChecks = recommendedChecks;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
