package com.traceroot.platform.search;

import com.traceroot.platform.common.Level;
import com.traceroot.platform.common.ResourceNotFoundException;
import com.traceroot.platform.ingestion.LogRecord;
import com.traceroot.platform.ingestion.LogResponse;
import com.traceroot.platform.ingestion.LogNormalizer;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class LogSearchService {
    final private LogRepository logRepository;
    final private LogNormalizer normalizer;

    public LogSearchService(LogRepository logRepository,
                            LogNormalizer normalizer) {
        this.logRepository = logRepository;
        this.normalizer = normalizer;
    }

    public List<LogResponse> getAllRecords() {
        return logRepository.findAll().stream().map(this::toLogResponse).collect(Collectors.toList());
    }

    public LogResponse getRecordById(UUID id) {
        return logRepository
                .findById(id)
                .map(this::toLogResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));
    }

    public List<LogResponse> getLogsByServiceNameAndLevel(String serviceName, Level level) {
        return logRepository.findByServiceNameAndLevel(
                normalizer.normServiceName(serviceName),
                normalizer.normalizeLevel(level)).stream().map(this::toLogResponse).collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByLevel(Level level) {
        return logRepository.findByLevel(normalizer.normalizeLevel(level)).stream()
                .map(this::toLogResponse).collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByServiceName(String serviceName) {
        return logRepository.findByServiceName(normalizer.normServiceName(serviceName)).stream().map(this::toLogResponse).collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByType(String serviceName, Level level, String exceptionType, String endpoint) {
        return logRepository.findByServiceNameAndLevelAndExceptionTypeAndEndpoint(
                serviceName, level, exceptionType, endpoint)
                .stream()
                .sorted(Comparator.comparing(LogRecord::getTimestamp))
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }

    private LogResponse toLogResponse(LogRecord rec) {
        return new LogResponse(rec.getId(),
                rec.getLevel(),
                rec.getServiceName(),
                rec.getMessage(),
                rec.getTimestamp(),
                rec.getEnvironment(),
                rec.getTraceId(),
                rec.getEndpoint(),
                rec.getExceptionType(),
                rec.getVersion(),
                rec.getCreatedAt());
    }
}
