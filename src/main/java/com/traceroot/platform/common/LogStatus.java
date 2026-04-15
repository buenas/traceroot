package com.traceroot.platform.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LogStatus {
    CREATED,PENDING, FAILED;

    @JsonCreator
    public static LogStatus fromString(String status) {
        return LogStatus.valueOf(status.toUpperCase());
    }
}
