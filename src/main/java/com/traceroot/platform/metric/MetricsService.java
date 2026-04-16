package com.traceroot.platform.metric;

import com.traceroot.platform.common.IncidentStatus;
import com.traceroot.platform.incident.IncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricsService {
    private final IncidentRepository incidentRepository;

    public MetricsService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public IncidentMetricsResponse getIncidentMetricsSummary() {

        long activeIncidents = incidentRepository.countIncidentsByIncidentStatus(IncidentStatus.ACTIVE);
        long resolvedIncidents = incidentRepository.countIncidentsByIncidentStatus(IncidentStatus.RESOLVED);
        long totalIncidentCount = incidentRepository.count();
        long incidentsCreatedLast24Hours = incidentRepository.countIncidentsByCreatedAtAfter(LocalDateTime.now().minusDays(1));
        long incidentsResolvedLast24Hours = incidentRepository.countIncidentsByResolvedAtAfter(LocalDateTime.now().minusDays(1));
        Double averageResolutionMinutes = incidentRepository.findAverageResolutionMinutes();
        if (averageResolutionMinutes == null) {
            averageResolutionMinutes = 0.0;
        }else {
            averageResolutionMinutes = BigDecimal.valueOf(averageResolutionMinutes)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new IncidentMetricsResponse(
                activeIncidents,
                resolvedIncidents,
                totalIncidentCount,
                incidentsCreatedLast24Hours,
                incidentsResolvedLast24Hours,
                averageResolutionMinutes);
    }

    public List<ServiceIncidentMetricsResponse> getIncidentMetricsByService() {
        return incidentRepository.getIncidentMetricsByService()
                .stream()
                .map(view -> new ServiceIncidentMetricsResponse(
                        view.getServiceName(),
                        view.getIncidentCount(),
                        view.getActiveCount(),
                        view.getResolvedCount()
                ))
                .toList();
    }

    public List<TopIncidentPatternResponse> findTopIncidentPatterns() {
        return incidentRepository.findTopIncidentPatterns()
                .stream()
                .map(view -> new TopIncidentPatternResponse(
                        view.getFingerPrint(),
                        view.getTitle(),
                        view.getIncidentCount()
                ))
                .toList();
    }
}
