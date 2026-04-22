package com.traceroot.platform.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class OpenAiRequestBuilder {
    private final ObjectMapper objectMapper;


    public OpenAiRequestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the full Chat Completions request body.
     *
     * @param model  the OpenAI model name (e.g., "gpt-4o-mini")
     * @param prompt the fully-rendered prompt from IncidentPromptBuilder
     * @return a JSON string ready to send as the HTTP request body
     */
    public String build(String model, String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);

        // The messages array. A single "user" message is simpler
        // than system+user splits and produces equivalent results
        // for our structured-output use case.
        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        // Structured Outputs specification. This is the key feature:
        // OpenAI constrains generation to match this schema, so we
        // never get free-text responses that our parser can't handle.
        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "incident_summary");
        jsonSchema.put("strict", true);  // strict=true makes schema enforcement absolute

        ObjectNode schema = jsonSchema.putObject("schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        // Required fields — all three must be present.
        ArrayNode required = schema.putArray("required");
        required.add("summary");
        required.add("possibleCause");
        required.add("recommendedChecks");

        // Properties definitions.
        ObjectNode properties = schema.putObject("properties");

        ObjectNode summaryProp = properties.putObject("summary");
        summaryProp.put("type", "string");
        summaryProp.put("description", "1-2 sentence technical summary of the incident pattern");

        ObjectNode causeProp = properties.putObject("possibleCause");
        causeProp.put("type", "string");
        causeProp.put("description", "2-3 sentence hypothesis on root cause based on logs");

        ObjectNode checksProp = properties.putObject("recommendedChecks");
        checksProp.put("type", "array");
        ObjectNode checksItems = checksProp.putObject("items");
        checksItems.put("type", "string");
        checksProp.put("description", "3-5 specific actionable checks for an on-call engineer");

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // This shouldn't happen — we constructed the tree ourselves.
            // Throwing as IllegalStateException because it indicates a
            // programmer error, not a runtime failure.
            throw new IllegalStateException("Failed to serialize OpenAI request", e);
        }
    }

}
