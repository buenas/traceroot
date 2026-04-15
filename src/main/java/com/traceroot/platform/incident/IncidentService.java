package com.traceroot.platform.incident;

import com.traceroot.platform.ai.IncidentSummaryResponse;
import com.traceroot.platform.ai.IncidentSummaryService;
import com.traceroot.platform.common.IncidentStatus;
import com.traceroot.platform.common.Level;
import com.traceroot.platform.common.ResourceNotFoundException;
import com.traceroot.platform.ingestion.LogResponse;
import com.traceroot.platform.search.LogSearchService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentHelper incidentHelper;
    private final LogSearchService logSearchService;
    private final IncidentSummaryService incidentSummaryService;

    public IncidentService(IncidentRepository incidentRepository, 
                           IncidentHelper incidentHelper,
                           LogSearchService logSearchService, 
                           IncidentSummaryService incidentSummaryService) {
        this.incidentRepository = incidentRepository;
        this.incidentHelper = incidentHelper;
        this.logSearchService = logSearchService;
        this.incidentSummaryService = incidentSummaryService;
    }

    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toIncidentResponse)
                .collect(Collectors.toList());
    }

    public IncidentResponse getIncidentsById(UUID id) {
        return incidentRepository.findById(id)
                .map(this::toIncidentResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not id: " + id));
    }

    public IncidentResponse updateIncident(UUID id) {
        Incident incident = getIncident(id);
        incident.setIncidentStatus(IncidentStatus.RESOLVED);
        incident.setUpdatedAt(LocalDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);
        return toIncidentResponse(savedIncident);
    }

    public boolean updateActiveIncidentIfExists(String fingerPrint) {
        // Look for an ACTIVE incident with the same fingerprint.
        Incident incident = incidentRepository.findByFingerPrintAndIncidentStatus(fingerPrint,
                IncidentStatus.ACTIVE);

        // No active incident found -> caller should create a new one.
        if (incident == null) {
            return false;
        }
        //update eventCount and lastSeen
        incident.setEventCount(incident.getEventCount() + 1);
        incident.setLastSeenAt(LocalDateTime.now());
        incident.setSummaryStale(true);

        incidentRepository.save(incident);
        return true;
    }

    public void createIncident(String fingerPrint, int eventCount, LocalDateTime firstSeenAt) {
        String[] parts = fingerPrint.split("\\|");
        createIncident(fingerPrint, parts, eventCount, firstSeenAt);
    }

    public IncidentSummaryResponse getIncidentSummary(UUID incidentId) {
        // Load the incident or fail fast.
        Incident incident = getIncident(incidentId);

        // Case 1:
        // Summary already exists and is fresh -> return persisted values.
        if (hasUsableSummary(incident)) {
            return mapToSummaryResponse(incident);
        }

        // Pull the logs used to build the prompt.
        // This keeps summary generation aligned with the incident pattern definition.
        List<LogResponse> logs = logSearchService.getLogsByType(
                incident.getServiceName(),
                incident.getLevel(),
                incident.getExceptionType(),
                incident.getEndpoint()
        );

        // Ask the AI summary service to generate a structured response
        // using the existing stub + prompt builder flow.
        IncidentSummaryResponse generated = incidentSummaryService.getIncidentSummary(logs, incident);

        // Case 2:
        // Summary exists but became stale -> overwrite persisted summary fields.
        if (hasStaleSummary(incident)) {
            return incidentSummaryService.persistSummary(generated, incident);
        }
        // Case 3:
        // No persisted summary exists yet -> save it for the first time.
        return incidentSummaryService.persistSummary(generated, incident);
    }

    public List<LogResponse> getIncidentDetails(UUID id) {
        Incident incident = getIncident(id);
        return logSearchService.getLogsByType(
                incident.getServiceName(),
                incident.getLevel(),
                incident.getExceptionType(),
                incident.getEndpoint());
    }

    private boolean hasStaleSummary(Incident incident) {
        // Summary content exists, but it has been marked stale because the
        // incident changed after the summary was generated.
        return incident.getSummary() != null &&
                incident.getPossibleCause() != null &&
                incident.getRecommendedChecks() != null &&
                incident.isSummaryStale();
    }

    private void createIncident(String fingerPrint, String[] parts, int eventCount, LocalDateTime firstSeenAt) {
        Incident newIncident = new Incident();
        String incidentTitle = incidentHelper.generateIncidentTitle(parts);

        newIncident.setFingerPrint(fingerPrint);
        newIncident.setIncidentStatus(IncidentStatus.ACTIVE);
        newIncident.setFirstSeenAt(firstSeenAt);
        newIncident.setCreatedAt(LocalDateTime.now());
        newIncident.setLastSeenAt(LocalDateTime.now());
        newIncident.setEventCount(eventCount);
        newIncident.setServiceName(parts[0]);
        newIncident.setLevel(Level.valueOf(parts[1]));
        newIncident.setExceptionType(parts[2]);
        newIncident.setEndpoint(parts[3]);
        newIncident.setTitle(incidentTitle);

        // New incident has no summary yet.
        // Set stale=true so the first summary request knows generation is required.
        newIncident.setSummaryStale(true);
        newIncident.setSummary(null);
        newIncident.setPossibleCause(null);
        newIncident.setRecommendedChecks(null);
        newIncident.setSummaryGeneratedAt(null);

        incidentRepository.save(newIncident);
    }

    private IncidentResponse toIncidentResponse(Incident incident) {
        return new IncidentResponse(incident.getId(),
                incident.getIncidentStatus(),
                incident.getTitle(),
                incident.getServiceName(),
                incident.getExceptionType(),
                incident.getEndpoint(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),
                incident.getEventCount());
    }

    private Incident getIncident(UUID id) {
        return incidentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident not id: " + id));
    }

    private boolean hasUsableSummary(Incident incident) {
        return incident.getSummary() != null &&
                incident.getPossibleCause() != null &&
                incident.getRecommendedChecks() != null &&
                !incident.isSummaryStale();
    }

    private IncidentSummaryResponse mapToSummaryResponse(Incident incident) {
        return new IncidentSummaryResponse(
                incident.getId(),
                incident.getSummary(),
                incident.getPossibleCause(),
                incident.getRecommendedChecks(),
                incident.getSummaryGeneratedAt()
        );
    }
}
