package com.traceroot.platform.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class StubLlmClient implements LlmClient {

    @Override
    public String generate(String prompt) {
        return """
                {
                  "summary": "Repeated backend errors are occurring for this incident pattern within the recent detection window.",
                  "possibleCause": "The logs suggest a recurring failure affecting the same service and endpoint, likely caused by an unstable downstream dependency or repeated timeout/error condition.",
                  "recommendedChecks": [
                    "Inspect the failing service logs around the first and last seen timestamps.",
                    "Check recent deployments or configuration changes affecting the service and endpoint.",
                    "Verify downstream dependency health, timeouts, and retry behavior."
                  ]
                }
                """;
    }
}