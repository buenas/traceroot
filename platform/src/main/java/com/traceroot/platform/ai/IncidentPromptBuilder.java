package com.traceroot.platform.ai;

import com.traceroot.platform.incident.Incident;
import com.traceroot.platform.ingestion.LogResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentPromptBuilder {

    private static final int MAX_LOGS = 10;

    public String buildPrompt(List<LogResponse> logs, Incident incident) {

        String formattedLogs = formatLogs(logs);

        return """
                ROLE
                You are a senior reliability engineer analyzing a production incident.
                Your output will be read by an on-call engineer who needs to decide
                whether to act and what to investigate first.
                
                TASK
                Analyze the incident below and produce a JSON object with three fields:
                summary, possibleCause, recommendedChecks.
                
                REASONING RULES
                - Ground every claim in the provided metadata or logs. Do not speculate
                  beyond the evidence. If the evidence is thin, say so explicitly in
                  possibleCause.
                - Use the First Seen At and Last Seen At timestamps. Note whether
                  errors are clustered (seconds/minutes apart) or spread across a
                  longer window, and what that pattern suggests.
                - Reference specific log messages, trace IDs, or log fields when
                  supporting a hypothesis. Do not reason from exception type and
                  endpoint alone.
                - Identify patterns across logs: same trace ID, repeated request
                  shapes, correlated timestamps with upstream/downstream services.
                  Name the pattern if you find one.
                - Prefer "likely" or "consistent with" over "may" or "possibly" when
                  the evidence supports it. Reserve hedging for genuine ambiguity.
                
                OUTPUT FORMAT
                summary: 1-2 sentences describing what is happening. Include the
                  service, exception type, event count, and time window. No marketing
                  tone.
                
                possibleCause: 2-3 sentences stating the most likely root cause and
                  the evidence that supports it. Cite specific log content or patterns.
                  If genuinely unclear, state what additional data would resolve the
                  ambiguity.
                
                recommendedChecks: 3-5 concrete investigation steps. Each step names
                  a specific component, file path, endpoint, query, dashboard, or
                  configuration to inspect. Do not include generic advice like
                  "check logs" or "review the code." Describe what to check, not
                  what to fix.
                
                Return only valid JSON. No markdown, code fences, or prose outside
                the JSON object.
                
                INCIDENT METADATA
                Title: %s
                Service: %s
                Level: %s
                Exception Type: %s
                Endpoint: %s
                Event Count: %d
                Status: %s
                First Seen At: %s
                Last Seen At: %s
                
                LOGS
                %s
                """.formatted(
                safe(incident.getTitle()),
                safe(incident.getServiceName()),
                incident.getLevel() == null ? "UNKNOWN" : incident.getLevel().name(),
                safe(incident.getExceptionType()),
                safe(incident.getEndpoint()),
                incident.getEventCount() == null ? 0 : incident.getEventCount(),
                incident.getIncidentStatus() == null ? "UNKNOWN" : incident.getIncidentStatus().name(),
                String.valueOf(incident.getFirstSeenAt()),
                String.valueOf(incident.getLastSeenAt()),
                formattedLogs
        );
    }

    private String formatLogs(List<LogResponse> logs) {
        if (logs == null || logs.isEmpty()) {
            return "No logs were provided for this incident.";
        }

        return logs.stream()
                .limit(MAX_LOGS)
                .map(this::formatSingleLog)
                .collect(Collectors.joining("\n"));
    }

    private String formatSingleLog(LogResponse log) {
        return """
                - timestamp: %s
                  level: %s
                  serviceName: %s
                  message: %s
                  exceptionType: %s
                  endpoint: %s
                  traceId: %s
                """.formatted(
                String.valueOf(log.getTimestamp()),
                log.getLevel() == null ? "UNKNOWN" : log.getLevel().name(),
                safe(log.getServiceName()),
                safe(log.getMessage()),
                safe(log.getExceptionType()),
                safe(log.getEndpoint()),
                safe(log.getTraceId())
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
    }
}

