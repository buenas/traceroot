package com.traceroot.platform.incident;

import com.traceroot.platform.common.IncidentStatus;
import com.traceroot.platform.metric.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByOrderByCreatedAtDesc();

    Incident findByFingerPrintAndIncidentStatus(String fingerPrint, IncidentStatus incidentStatus);

    Optional<Incident> findFirstByFingerPrintAndIncidentStatusOrderByResolvedAtDesc(String fingerPrint, IncidentStatus incidentStatus);

    long countIncidentsByIncidentStatus(IncidentStatus incidentStatus);

    long countIncidentsByCreatedAtAfter(LocalDateTime time);

    long countIncidentsByResolvedAtAfter(LocalDateTime time);


    @Query(value = """
            SELECT
                finger_print AS fingerPrint,
                title AS title,
                COUNT(*) AS incidentCount
            FROM incident
            GROUP BY finger_print, title
            ORDER BY incidentCount DESC
            """, nativeQuery = true)
    List<TopIncidentPatternView> findTopIncidentPatterns();

    @Query(value = """
            SELECT
                service_name AS serviceName,
                COUNT(*) AS incidentCount,
                SUM(CASE WHEN incident_status = 'ACTIVE' THEN 1 ELSE 0 END) AS activeCount,
                SUM(CASE WHEN incident_status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolvedCount
            FROM incident
            GROUP BY service_name
            ORDER BY incidentCount DESC
            """, nativeQuery = true)
    List<ServiceIncidentMetricsView> getIncidentMetricsByService();

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 60.0)
            FROM incident
            WHERE incident_status = 'RESOLVED'
              AND resolved_at IS NOT NULL
              AND resolved_at >= created_at
            """, nativeQuery = true)
    Double findAverageResolutionMinutes();
}
