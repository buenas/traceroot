package com.traceroot.platform.incident;

import com.traceroot.platform.ingestion.LogRecord;
import org.springframework.stereotype.Service;

@Service
public class IncidentHelper {

    public static final String UNKNOWN_EXCEPTION = "UNKNOWN_EXCEPTION";
    public static final String UNKNOWN_ENDPOINT = "UNKNOWN_ENDPOINT";

    public IncidentHelper() {}


    public String buildPatternKey(LogRecord record) {
        String exceptionType = normalizeExceptionType(record.getExceptionType());
        String endpoint = normalizeEndpoint(record.getEndpoint());

        return record.getServiceName() + "|" +
                record.getLevel() + "|" +
                exceptionType + "|" +
                endpoint;
    }

    public String generateIncidentTitle(String[] parts) {
        String serviceName = parts[0];
        String exceptionType = normalizeExceptionType(parts[2]);
        String endpoint = normalizeEndpoint(parts[3]);

        boolean unknownException = UNKNOWN_EXCEPTION.equals(exceptionType);
        boolean unknownEndpoint = UNKNOWN_ENDPOINT.equals(endpoint);

        if (unknownException && unknownEndpoint) {
            return "Repeated error in " + serviceName;
        }
        if (unknownException) {
            return "Repeated error in " + serviceName + " " + endpoint;
        }
        if (unknownEndpoint) {
            return "Repeated " + exceptionType + " in " + serviceName;
        }
        return "Repeated " + exceptionType + " in " + serviceName + " " + endpoint;
    }

    private String normalizeExceptionType(String exceptionType) {
        if (exceptionType == null || exceptionType.trim().isEmpty()) {
            return UNKNOWN_EXCEPTION;
        }
        return exceptionType.trim();
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return UNKNOWN_ENDPOINT;
        }
        return endpoint.trim();
    }
}
