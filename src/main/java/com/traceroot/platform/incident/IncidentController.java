package com.traceroot.platform.incident;

import com.traceroot.platform.ai.IncidentSummaryResponse;
import com.traceroot.platform.ingestion.LogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping()
    public ResponseEntity<List<IncidentResponse>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentsById(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentsById(id));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<LogResponse>> getIncidentDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentDetails(id));
    }

    //return IncidentResponse
    @PostMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> markIncidentResolved(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.updateIncident(id));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<IncidentSummaryResponse> getIncidentSummary(@PathVariable UUID id){
        return ResponseEntity.ok(incidentService.getIncidentSummary(id));
    }
}
