package com.traceroot.platform.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes benchmark results to disk as JSON.
 *
 */
public class BenchmarkResultWriter {

    private static final DateTimeFormatter FOLDER_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");

    private static final Path OUTPUT_ROOT = Paths.get("benchmark-output");

    private final ObjectMapper objectMapper;
    private final Path runFolder;

    public BenchmarkResultWriter(LocalDateTime runTimestamp) throws IOException {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String folderName = runTimestamp.format(FOLDER_TIMESTAMP);
        this.runFolder = OUTPUT_ROOT.resolve(folderName);
        Files.createDirectories(runFolder);
    }

    public void writeQueryResult(int queryNumber, String queryName, Map<String, Object> result)
            throws IOException {
        String fileName = String.format("query-%d-%s.json",
                queryNumber,
                queryName.toLowerCase().replace(' ', '-'));
        Path filePath = runFolder.resolve(fileName);
        objectMapper.writeValue(filePath.toFile(), result);
    }

    public void writeIndex(Map<String, Object> rowCounts, List<Map<String, Object>> querySummaries)
            throws IOException {
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("runTimestamp", runFolder.getFileName().toString());
        index.put("rowCounts", rowCounts);
        index.put("queries", querySummaries);
        Path indexPath = runFolder.resolve("_index.json");
        objectMapper.writeValue(indexPath.toFile(), index);
    }

    public Path getRunFolder() {
        return runFolder;
    }
}