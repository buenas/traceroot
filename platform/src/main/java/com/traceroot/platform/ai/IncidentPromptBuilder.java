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
                You are an expert backend engineer and incident response assistant.
                
                Your task is to analyze a backend system incident based only on the structured incident metadata and logs provided below.
                
                Rules:
                - Be precise.
                - Do not speculate beyond the provided data.
                - Base your reasoning only on the logs and metadata.
                - If the evidence is limited, say so.
                - Return ONLY valid JSON.
                - Do not include markdown, code fences, or explanatory text outside JSON.
                
                ## Incident Metadata
                Title: %s
                Service: %s
                Level: %s
                Exception Type: %s
                Endpoint: %s
                Event Count: %d
                Status: %s
                First Seen At: %s
                Last Seen At: %s
                
                ## Logs
                %s
                
                ## Required Output Format
                {
                  "summary": "...",
                  "possibleCause": "...",
                  "recommendedChecks": [
                    "...",
                    "...",
                    "..."
                  ]
                }
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

