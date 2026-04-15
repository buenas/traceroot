package com.traceroot.platform.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum IncidentStatus {
    ACTIVE, RESOLVED;

    @JsonCreator
    public static IncidentStatus fromString(String status) {
        return IncidentStatus.valueOf(status.toUpperCase());
    }
}
