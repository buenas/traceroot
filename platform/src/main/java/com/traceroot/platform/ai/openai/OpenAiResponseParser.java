package com.traceroot.platform.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenAiResponseParser {
    private final ObjectMapper objectMapper;

    public OpenAiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses the raw HTTP response body from OpenAI into an LlmSummary.
     *
     * @param responseBody the raw JSON string from the HTTP response
     * @return the structured summary
     * @throws LlmResponseParseException if the response doesn't match
     *         the expected shape
     */
    public String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmResponseParseException(
                        "OpenAI response had no choices: " + responseBody);
            }

            JsonNode messageContent = choices.get(0).path("message").path("content");
            if (!messageContent.isTextual()) {
                throw new LlmResponseParseException(
                        "OpenAI response missing message.content: " + responseBody);
            }

            // Return the content string as-is. IncidentSummaryService
            // will parse this string into an IncidentSummaryResponse.
            return messageContent.asText();

        } catch (LlmResponseParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmResponseParseException(
                    "Failed to parse OpenAI response envelope: " + responseBody, e);
        }
    }
}
