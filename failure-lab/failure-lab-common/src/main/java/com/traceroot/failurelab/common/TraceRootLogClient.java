package com.traceroot.failurelab.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;

@Component
public class TraceRootLogClient {

    private final RestTemplate restTemplate;
    private final String ingestionUrl;
    private final Environment environment;
    private final String version;

    public TraceRootLogClient(@Value("${traceroot.ingestion.url}") String ingestionUrl,
                              @Value("${failure.lab.environment:prod}") String environment,
                              @Value("${failure.lab.version:1.0.0}") String version) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restTemplate = new RestTemplate(factory);
        this.ingestionUrl = ingestionUrl;
        this.environment = Environment.fromString(environment);
        this.version = version;
    }

    /**
     * Sends a structured log to TraceRoot.
     * This is the core contract between the Failure Lab and the TraceRoot platform.
     */
    public void sendLog(Level level,
                        String serviceName,
                        String message,
                        String endpoint,
                        String exceptionType,
                        String traceId) {
        LogEventRequest request = new LogEventRequest();
        request.setLevel(level);
        request.setServiceName(serviceName);
        request.setMessage(message);
        request.setEnvironment(environment);
        request.setEndpoint(endpoint);
        request.setExceptionType(exceptionType);
        request.setTraceId(traceId);
        request.setVersion(version);
        request.setTimestamp(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(
                ingestionUrl,
                new HttpEntity<>(request, headers),
                Void.class
        );
    }
}