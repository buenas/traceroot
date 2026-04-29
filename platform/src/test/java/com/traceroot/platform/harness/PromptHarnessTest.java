package com.traceroot.platform.harness;

import com.traceroot.platform.ai.IncidentSummaryResponse;
import com.traceroot.platform.ai.IncidentSummaryService;
import com.traceroot.platform.incident.Incident;
import com.traceroot.platform.incident.IncidentRepository;
import com.traceroot.platform.incident.IncidentResponse;
import com.traceroot.platform.ai.IncidentPromptBuilder;
import com.traceroot.platform.ingestion.LogResponse;
import com.traceroot.platform.search.LogSearchService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Prompt A/B harness.
 *
 * Runs every incident in the database through the current prompt and LLM,
 * writing results to harness-output/{timestamp}/. Use to compare prompt
 * iterations side-by-side.
 *
 * IMPORTANT:
 * - Tagged "harness". Excluded from default `mvn test` runs (see pom.xml).
 * - Requires a real LLM profile to be active (openai or anthropic).
 *   Does NOT run against the stub — the whole point is to see real output.
 * - Bypasses IncidentService.getIncidentSummary() caching. Calls
 *   IncidentSummaryService directly. Does not persist summaries to the DB.
 *
 * To run:
 *   From IntelliJ: right-click class → Run. Set run config:
 *     - Active profile: openai
 *     - Tags: harness
 *     - Env var: OPENAI_API_KEY
 *   From Maven:
 *     mvn test -Dgroups=harness -Dspring.profiles.active=openai
 *
 * Why a @SpringBootTest and not a plain @Test:
 *   Spring wires up IncidentSummaryService with its real collaborators
 *   (prompt builder, LLM client, object mapper). Zero mocking. The harness
 *   exercises the same code path as production, minus the caching check.
 */
@SpringBootTest
@Tag("harness")
public class PromptHarnessTest {

    private static final Logger log = LoggerFactory.getLogger(PromptHarnessTest.class);

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentSummaryService incidentSummaryService;

    @Autowired
    private IncidentPromptBuilder promptBuilder;

    @Autowired
    private LogSearchService logSearchService;

    @Autowired
    private Environment environment;

    @Value("${traceroot.llm.openai.model:unknown}")
    private String openAiModel;

    @Test
    void runHarnessAcrossAllIncidents() throws Exception {
        LocalDateTime runTimestamp = LocalDateTime.now();
        HarnessOutputWriter writer = new HarnessOutputWriter(runTimestamp);

        log.info("Harness run starting. Output folder: {}", writer.getRunFolder());

        List<Incident> incidents = incidentRepository.findAll();
        if (incidents.isEmpty()) {
            log.warn("No incidents in DB. Harness run produces no output.");
            return;
        }

        log.info("Running {} incidents through the current prompt.", incidents.size());

        List<HarnessRunResult> results = new ArrayList<>();

        for (Incident incident : incidents) {
            log.info("Processing incident {} ({})", incident.getId(), incident.getTitle());
            try {
                HarnessRunResult result = runSingleIncident(incident, runTimestamp);
                writer.writeIncidentResult(result);
                results.add(result);
            } catch (Exception e) {
                // One incident failing shouldn't tank the whole harness run.
                // Log, skip, continue — the remaining incidents still give signal.
                log.error("Failed to process incident {}: {}",
                        incident.getId(), e.getMessage(), e);
            }
        }

        writer.writeIndex(results);

        log.info("Harness run complete. {} successful, {} failed. Output: {}",
                results.size(),
                incidents.size() - results.size(),
                writer.getRunFolder());
    }

    /**
     * Runs a single incident through the prompt + LLM pipeline.
     *
     * Deliberately bypasses IncidentService.getIncidentSummary() which
     * would short-circuit on non-stale summaries. We want every incident
     * to hit the LLM on every harness run — that's the point of the A/B
     * comparison.
     *
     * Builds the prompt directly here (rather than via IncidentSummaryService)
     * so we can capture it for the output. The service internally builds
     * the same prompt and throws it away after the LLM call.
     */
    private HarnessRunResult runSingleIncident(Incident incident,
                                               LocalDateTime timestamp) {
        // Load the logs that match this incident's fingerprint.
        List<LogResponse> logs = logSearchService.getLogsByType(
                incident.getServiceName(),
                incident.getLevel(),
                incident.getExceptionType(),
                incident.getEndpoint()
        );

        // Render the prompt — captured so we can inspect it in the output.
        String prompt = promptBuilder.buildPrompt(logs, incident);

        // Time the full LLM call including parsing.
        long startNanos = System.nanoTime();
        IncidentSummaryResponse output =
                incidentSummaryService.getIncidentSummary(logs, incident);
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        // Resolve the active profile for provenance.
        String activeProfile = String.join(",", environment.getActiveProfiles());

        return new HarnessRunResult(
                toIncidentResponse(incident),
                logs,
                prompt,
                output,
                latencyMs,
                openAiModel,
                activeProfile,
                timestamp
        );
    }

    /**
     * Maps the Incident entity to IncidentResponse.
     * Duplicates logic from IncidentService.toIncidentResponse, but we
     * can't easily call that private method from here. Short enough
     * that duplication is cheaper than refactoring to expose it.
     */
    private IncidentResponse toIncidentResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getIncidentStatus(),
                incident.getTitle(),
                incident.getServiceName(),
                incident.getExceptionType(),
                incident.getEndpoint(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),
                incident.getEventCount()
        );
    }
}