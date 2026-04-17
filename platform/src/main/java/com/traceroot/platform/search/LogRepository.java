package com.traceroot.platform.search;

import com.traceroot.platform.common.Level;
import com.traceroot.platform.ingestion.LogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LogRepository extends JpaRepository<LogRecord, UUID> {
    List<LogRecord> findByServiceNameAndLevel(String serviceName, Level level);
    List<LogRecord> findByLevel(Level level);
    List<LogRecord> findByServiceName(String serviceName);
    List<LogRecord> findByServiceNameAndLevelAndExceptionTypeAndEndpointAndTimestampAfter(String serviceName, Level level, String exceptionType, String endpoint, LocalDateTime timestampAfter);
    List<LogRecord> findByServiceNameAndLevelAndExceptionTypeAndEndpoint(String serviceName, Level level, String exceptionType, String endpoint);
}
