package com.traceroot.platform.ingestion;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class LogIngestionController {

    private final LogService logService;

    public LogIngestionController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/logs")
    public ResponseEntity<LogRecord> PostLogs(@Valid @RequestBody LogEventRequest request){
        LogRecord savedRecord = logService.createLog(request);
        return ResponseEntity.status(201).body(savedRecord);
    }
}
