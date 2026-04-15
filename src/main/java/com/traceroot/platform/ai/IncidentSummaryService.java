package com.traceroot.platform.ai;

import com.traceroot.platform.incident.Incident;
import com.traceroot.platform.incident.IncidentRepository;
import com.traceroot.platform.ingestion.LogResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class IncidentSummaryService {
    private final IncidentPromptBuilder promptBuilder;
    private final StubLlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final IncidentRepository incidentRepository;

    public IncidentSummaryService(IncidentPromptBuilder promptBuilder,
                                  StubLlmClient llmClient,
                                  ObjectMapper objectMapper, IncidentRepository incidentRepository) {
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.incidentRepository = incidentRepository;
    }

    public IncidentSummaryResponse getIncidentSummary(List<LogResponse> list, Incident incident) {
        String prompt = promptBuilder.buildPrompt(list, incident);
        String rawResponse = llmClient.generate(prompt);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String summary = getText(root, "summary");
            String possibleCause = getText(root, "possibleCause");

            List<String> recommendedChecks = new ArrayList<>();
            JsonNode checksNode = root.get("recommendedChecks");
            if (checksNode != null && checksNode.isArray()) {
                for (JsonNode check : checksNode) {
                    recommendedChecks.add(check.asText());
                }
            }
            return new IncidentSummaryResponse(
                    incident.getId(),
                    summary,
                    possibleCause,
                    recommendedChecks,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private String getText(JsonNode root, String fieldName) {
        JsonNode field = root.get(fieldName);
        return field == null || field.isNull() ? "" : field.asText();
    }

    public IncidentSummaryResponse persistSummary(IncidentSummaryResponse summary, Incident incident) {
        incident.setSummary(summary.getSummary());
        incident.setPossibleCause(summary.getPossibleCause());
        incident.setRecommendedChecks(summary.getRecommendedChecks());
        incident.setSummaryGeneratedAt(LocalDateTime.now());
        incident.setSummaryStale(false);
        incidentRepository.save(incident);
        return summary;
    }
}

