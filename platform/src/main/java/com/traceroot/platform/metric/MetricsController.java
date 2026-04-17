package com.traceroot.platform.metric;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics/incidents")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<IncidentMetricsResponse> getIncidentMetricsSummary() {
        return ResponseEntity.ok(metricsService.getIncidentMetricsSummary());
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceIncidentMetricsResponse>> retrieveGroupIncidentsCounts() {
        return ResponseEntity.ok(metricsService.getIncidentMetricsByService());
    }

    @GetMapping("/top-patterns")
    public ResponseEntity<List<TopIncidentPatternResponse>> getRepeatedIncidentFingerprints() {
        return ResponseEntity.ok(metricsService.findTopIncidentPatterns());
    }
}
