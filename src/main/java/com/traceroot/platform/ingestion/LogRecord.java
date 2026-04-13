package com.traceroot.platform.ingestion;


import com.traceroot.platform.common.Environment;
import com.traceroot.platform.common.Level;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="logs")
public class LogRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Level level;
    private String serviceName;
    private String message;
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private Environment environment;
    private String traceId;
    private String endpoint;
    private String exceptionType;
    private String version;
    private LocalDateTime createdAt;

    public LogRecord() {}

    public LogRecord(UUID id,
                     Level level,
                     String serviceName,
                     String message,
                     LocalDateTime timestamp,
                     Environment environment, String traceId, String endpoint,
                     String exceptionType, String version, LocalDateTime createdAt) {
        this.id = id;
        this.level = level;
        this.serviceName = serviceName;
        this.message = message;
        this.timestamp = timestamp;
        this.environment = environment;
        this.traceId = traceId;
        this.endpoint = endpoint;
        this.exceptionType = exceptionType;
        this.version = version;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "id=" + id +
                ", level='" + level + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", environment='" + environment + '\'' +
                ", traceId='" + traceId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", version='" + version + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
