package com.traceroot.platform.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Writes harness run results to disk. Each run gets a fresh timestamped folder.
 */
public class HarnessOutputWriter {

    private static final DateTimeFormatter FOLDER_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");
    private static final Path OUTPUT_ROOT = Paths.get("harness-output");
    private final ObjectMapper objectMapper;
    private final Path runFolder;

    /**
     * Initializes the writer and creates the run folder on disk.
     * The folder name is the current timestamp. Run this at the start
     * of the harness test; all writes in the run go into this folder.
     */
    public HarnessOutputWriter(LocalDateTime runTimestamp) throws IOException {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String folderName = runTimestamp.format(FOLDER_TIMESTAMP);
        this.runFolder = OUTPUT_ROOT.resolve(folderName);
        Files.createDirectories(runFolder);
    }

    /**
     * Writes a single incident's result as {incidentId}.json in the run folder.
     */
    public void writeIncidentResult(HarnessRunResult result) throws IOException {
        String fileName = result.incident().getId() + ".json";
        Path filePath = runFolder.resolve(fileName);
        objectMapper.writeValue(filePath.toFile(), result);
    }

    /**
     * Writes _index.json summarizing the whole run.
     * Meant as a one-glance view: which incidents ran, how long each took, and a truncated summary line from each output.
     */
    public void writeIndex(List<HarnessRunResult> results) throws IOException {
        List<Map<String, Object>> entries = results.stream()
                .map(r -> Map.<String, Object>of(
                        "incidentId", r.incident().getId().toString(),
                        "title", r.incident().getTitle(),
                        "latencyMs", r.latencyMs(),
                        "summaryPreview", truncate(r.output().getSummary(), 140)
                ))
                .toList();

        Map<String, Object> index = Map.of(
                "runTimestamp", runFolder.getFileName().toString(),
                "incidentCount", results.size(),
                "totalLatencyMs", results.stream().mapToLong(HarnessRunResult::latencyMs).sum(),
                "results", entries
        );

        Path indexPath = runFolder.resolve("_index.json");
        objectMapper.writeValue(indexPath.toFile(), index);
    }

    public Path getRunFolder() {
        return runFolder;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}