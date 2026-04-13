package com.traceroot.platform.incident;

import com.traceroot.platform.ai.IncidentPromptBuilder;
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
    private final IncidentPromptBuilder promptBuilder;
    private final IncidentSummaryService incidentSummaryService;

    public IncidentService(IncidentRepository incidentRepository, IncidentHelper incidentHelper, LogSearchService logSearchService, IncidentSummaryService incidentSummaryService, IncidentPromptBuilder promptBuilder) {
        this.incidentRepository = incidentRepository;
        this.incidentHelper = incidentHelper;
        this.logSearchService = logSearchService;
        this.incidentSummaryService = incidentSummaryService;
        this.promptBuilder = promptBuilder;
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
        //if exist increase eventCount and update lastSeen
        Incident incident = incidentRepository.findByFingerPrintAndIncidentStatus(fingerPrint, IncidentStatus.ACTIVE);
        if (incident == null) {
            return false;
        }
        //update eventCount and lastSeen
        incident.setEventCount(incident.getEventCount() + 1);
        incident.setLastSeenAt(LocalDateTime.now());
        incidentRepository.save(incident);
        return true;
    }

    public void createIncident(String fingerPrint, int eventCount, LocalDateTime firstSeenAt) {
        String[] parts = fingerPrint.split("\\|");
        createIncident(fingerPrint, parts, eventCount, firstSeenAt);
    }

    public List<LogResponse> getIncidentDetails(UUID id) {
        Incident incident = getIncident(id);
        return logSearchService.getLogsByType(
                incident.getServiceName(),
                incident.getLevel(),
                incident.getExceptionType(),
                incident.getEndpoint());
    }

    public IncidentSummaryResponse getIncidentSummary(UUID incidentId) {
        Incident incident = getIncident(incidentId);
        List<LogResponse> incidentList = getIncidentDetails(incident.getId());
        return incidentSummaryService.getIncidentSummary(incidentList, incident);
    }

    private void createIncident(String fingerPrint, String[] parts, int eventCount, LocalDateTime firstSeenAt) {
        Incident createIncident = new Incident();
        String incidentTitle = incidentHelper.generateIncidentTitle(parts);

        createIncident.setFingerPrint(fingerPrint);
        createIncident.setIncidentStatus(IncidentStatus.ACTIVE);
        createIncident.setFirstSeenAt(firstSeenAt);
        createIncident.setCreatedAt(LocalDateTime.now());
        createIncident.setLastSeenAt(LocalDateTime.now());
        createIncident.setEventCount(eventCount);
        createIncident.setServiceName(parts[0]);
        createIncident.setLevel(Level.valueOf(parts[1]));
        createIncident.setExceptionType(parts[2]);
        createIncident.setEndpoint(parts[3]);
        createIncident.setTitle(incidentTitle);
        incidentRepository.save(createIncident);
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

}
