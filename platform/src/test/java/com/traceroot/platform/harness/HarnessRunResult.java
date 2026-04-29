package com.traceroot.platform.harness;

import com.traceroot.platform.ai.IncidentSummaryResponse;
import com.traceroot.platform.incident.IncidentResponse;
import com.traceroot.platform.ingestion.LogResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Per-incident result of a harness run.
 * Captures:
 * - the incident metadata used as context
 * - the logs included in the prompt
 * - the rendered prompt string (so prompt changes are visible)
 * - the LLM output (parsed into IncidentSummaryResponse)
 * - timing and provenance metadata
 */
public record HarnessRunResult(
        IncidentResponse incident,
        List<LogResponse> logs,
        String prompt,
        IncidentSummaryResponse output,
        long latencyMs,
        String model,
        String profile,
        LocalDateTime timestamp) {}