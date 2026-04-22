package com.traceroot.platform.ai.openai;

import com.traceroot.platform.ai.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@Service
@Profile("openai")
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final OpenAiProperties properties;
    private final OpenAiRequestBuilder requestBuilder;
    private final OpenAiResponseParser responseParser;
    private final RestTemplate restTemplate;

    public OpenAiLlmClient(
            OpenAiProperties properties,
            OpenAiRequestBuilder requestBuilder,
            OpenAiResponseParser responseParser) {
        this.properties = properties;
        this.requestBuilder = requestBuilder;
        this.responseParser = responseParser;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());

        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @Retryable(
            retryFor = {
                    HttpServerErrorException.class,
                    ResourceAccessException.class,
                    HttpClientErrorException.TooManyRequests.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 3)
    )
    public String generate(String prompt) {
        log.debug("Calling OpenAI with model={}", properties.getModel());

        String requestBody = requestBuilder.build(properties.getModel(), prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                properties.getEndpoint(),
                HttpMethod.POST,
                request,
                String.class
        );

        log.debug("OpenAI response received, status={}", response.getStatusCode());

        // Extract the assistant message content (which is itself a JSON string).
        // Return it as-is — IncidentSummaryService will parse it into the DTO.
        return responseParser.extractContent(response.getBody());
    }

    @Recover
    public String recover(Exception e, String prompt) {
        log.warn("OpenAI unavailable after retries: {}", e.getMessage());

        return """
                {
                  "summary": "Summary temporarily unavailable. Please retry in a moment.",
                  "possibleCause": "Analysis pending — LLM service is currently unreachable.",
                  "recommendedChecks": [
                    "Retry this request shortly.",
                    "Check LLM provider status if failures persist."
                  ]
                }
                """;
    }
}
