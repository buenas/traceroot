package com.traceroot.platform.incident;

import com.traceroot.platform.common.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByOrderByCreatedAtDesc();
    Incident findByFingerPrintAndIncidentStatus(String fingerPrint, IncidentStatus incidentStatus);
    Optional<Incident> findFirstByFingerPrintAndIncidentStatusOrderByResolvedAtDesc(String fingerPrint, IncidentStatus incidentStatus);
}
