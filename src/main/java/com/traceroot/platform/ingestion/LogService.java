package com.traceroot.platform.ingestion;

import com.traceroot.platform.common.Level;
import com.traceroot.platform.incident.IncidentHelper;
import com.traceroot.platform.incident.IncidentService;
import com.traceroot.platform.search.LogRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class LogService {
    public static final int INCIDENT_THRESHOLD = 3;
    final private LogRepository logRepository;
    final private LogNormalizer logNormalizer;
    final private LogRecordMapper logRecordMapper;
    final private IncidentService incidentService;
    final private IncidentHelper incidentHelper;

    public LogService(LogRepository logRepository,
                      LogNormalizer logNormalizer,
                      LogRecordMapper logRecordMapper,
                      IncidentService incidentService,
                      IncidentHelper incidentHelper) {
        this.logRepository = logRepository;
        this.logNormalizer = logNormalizer;
        this.logRecordMapper = logRecordMapper;
        this.incidentService = incidentService;
        this.incidentHelper = incidentHelper;
    }

    public LogRecord createLog(LogEventRequest request) {
        LogRecord record = logRecordMapper.mapToRecord(logNormalizer.normalize(request));
        logRepository.save(record);
        if (!record.getLevel().equals(Level.ERROR)) {
            return record;
        }

        String fingerPrint = incidentHelper.buildPatternKey(record);

        //check for active incidents
        boolean activeUpdated = incidentService.updateActiveIncidentIfExists(fingerPrint);
        if (activeUpdated) return record;

        //check for resolved incidents
        boolean reopened = incidentService.updateResolvedIncidentIfExists(fingerPrint);
        if (reopened) return record;

        //count matching logs in log db for same fingerprint
        //test this
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(5);
        List<LogRecord> matchList =
                logRepository.findByServiceNameAndLevelAndExceptionTypeAndEndpointAndTimestampAfter(
                        record.getServiceName(),
                        record.getLevel(),
                        record.getExceptionType(),
                        record.getEndpoint(),
                        windowStart);

        //if >= 3 met create incident
        if (matchList.size() >= INCIDENT_THRESHOLD) {
            incidentService.createIncident(fingerPrint, matchList.size(), request.getTimestamp());
        }
        return record;
    }
}
