package com.traceroot.platform.search;

import com.traceroot.platform.common.Level;
import com.traceroot.platform.ingestion.LogRecord;
import com.traceroot.platform.ingestion.LogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LogSearchController {
    private final LogSearchService logSearchService;

    public LogSearchController(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<LogResponse> getLogsById(@PathVariable UUID id) {
        return ResponseEntity.ok(logSearchService.getRecordById(id));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogResponse>> getAllLogs(@RequestParam(value = "serviceName", required = false) String serviceName,
                                                @RequestParam(value = "level", required = false) Level level) {

        if (level != null && serviceName != null) return ResponseEntity.ok(logSearchService.getLogsByServiceNameAndLevel(serviceName, level));
        if (serviceName != null) return ResponseEntity.ok(logSearchService.getLogsByServiceName(serviceName));
        if (level != null) return ResponseEntity.ok(logSearchService.getLogsByLevel(level));
        return ResponseEntity.ok(logSearchService.getAllRecords());
    }


}
