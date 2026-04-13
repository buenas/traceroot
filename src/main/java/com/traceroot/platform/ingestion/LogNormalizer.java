package com.traceroot.platform.ingestion;

import com.traceroot.platform.common.Environment;
import com.traceroot.platform.common.Level;
import com.traceroot.platform.incident.IncidentHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogNormalizer {

    public LogNormalizer() {
    }

    public LogEventRequest normalize(LogEventRequest request) {
        request.setServiceName(normServiceName(request.getServiceName()));
        request.setMessage(normMessage(request.getMessage()));
        request.setLevel(normalizeLevel(request.getLevel()));
        request.setEnvironment(normalizeEnvironment(request.getEnvironment()));
        request.setTimestamp(normalizeTimestamp(request.getTimestamp()));
        request.setEndpoint(normalizeEndpoint(request.getEndpoint()));
        request.setExceptionType(normalizeExceptionType(request.getExceptionType()));
        request.setTraceId(normalizeString(request.getTraceId()));
        request.setVersion(normalizeString(request.getVersion()));
        return request;
    }

    public String normServiceName(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("ServiceName field cannot be empty");
        }
        return str.trim();
    }

    private String normMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message field cannot be empty");
        }
        return message.trim();
    }

    //endpoint, exceptionType, traceId, version
    private static String normalizeString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    public Level normalizeLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level field cannot be empty");
        }
        return Level.valueOf(level.name());
    }

    public Environment normalizeEnvironment(Environment env) {
        if (env == null) {
            throw new IllegalArgumentException("Environment field cannot be empty");
        }
        return Environment.valueOf(env.name());
    }

    public LocalDateTime normalizeTimestamp(LocalDateTime timestamp) {
        if (timestamp == null || timestamp.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Timestamp field cannot be empty or in the future");
        }
        return timestamp;
    }

    private String normalizeExceptionType(String exceptionType) {
        if (exceptionType == null || exceptionType.trim().isEmpty()) {
            return IncidentHelper.UNKNOWN_EXCEPTION;
        }
        return exceptionType.trim();
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return IncidentHelper.UNKNOWN_ENDPOINT;
        }
        return endpoint.trim();
    }

}
