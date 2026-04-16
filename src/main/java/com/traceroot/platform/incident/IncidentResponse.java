package com.traceroot.platform.incident;

import com.traceroot.platform.common.IncidentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class IncidentResponse {
    private UUID id;
    private IncidentStatus incidentStatus;
    private String title;
    private String serviceName;
    private String exceptionType;
    private String endpoint;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Integer eventCount;

    public IncidentResponse(UUID id,
                            IncidentStatus incidentStatus,
                            String title,
                            String serviceName,
                            String exceptionType,
                            String endpoint,
                            LocalDateTime firstSeenAt,
                            LocalDateTime lastSeenAt,
                            Integer eventCount){
        this.id = id;
        this.incidentStatus = incidentStatus;
        this.title = title;
        this.serviceName = serviceName;
        this.exceptionType = exceptionType;
        this.endpoint = endpoint;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.eventCount = eventCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public IncidentStatus getIncidentStatus() {
        return incidentStatus;
    }

    public void setIncidentStatus(IncidentStatus incidentStatus) {
        this.incidentStatus = incidentStatus;
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
}
