package com.traceroot.platform.ingestion;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogRecordMapper {

    public LogRecordMapper() {
    }

    public LogRecord mapToRecord(LogEventRequest request) {
        LogRecord record = new LogRecord();
        record.setLevel(request.getLevel());
        record.setMessage(request.getMessage());
        record.setServiceName(request.getServiceName());
        record.setTimestamp(request.getTimestamp());
        record.setEnvironment(request.getEnvironment());
        record.setTraceId(request.getTraceId());
        record.setEndpoint(request.getEndpoint());
        record.setExceptionType(request.getExceptionType());
        record.setVersion(request.getVersion());
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
