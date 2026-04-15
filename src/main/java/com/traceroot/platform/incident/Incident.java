package com.traceroot.platform.incident;

import com.traceroot.platform.common.IncidentStatus;
import com.traceroot.platform.common.Level;
import com.traceroot.platform.common.StringListConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="incident")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String title;
    private String serviceName;

    @Enumerated(EnumType.STRING)
    private Level level;
    private String exceptionType;
    private String endpoint;

    @Enumerated(EnumType.STRING)
    private IncidentStatus incidentStatus;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Integer eventCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fingerPrint;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String possibleCause;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> recommendedChecks;

    private LocalDateTime summaryGeneratedAt;

    @Column(nullable = false)
    private boolean summaryStale = true;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public IncidentStatus getIncidentStatus() {
        return incidentStatus;
    }

    public void setIncidentStatus(IncidentStatus incidentStatus) {
        this.incidentStatus = incidentStatus;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public void setFingerPrint(String fingerPrint) {
        this.fingerPrint = fingerPrint;
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

    public LocalDateTime getSummaryGeneratedAt() {
        return summaryGeneratedAt;
    }

    public void setSummaryGeneratedAt(LocalDateTime summaryGeneratedAt) {
        this.summaryGeneratedAt = summaryGeneratedAt;
    }

    public boolean isSummaryStale() {
        return summaryStale;
    }

    public void setSummaryStale(boolean summaryStale) {
        this.summaryStale = summaryStale;
    }
}
