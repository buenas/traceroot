package com.traceroot.failurelab.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Level {
    INFO,
    WARN,
    ERROR;
    @JsonCreator
    public static Level fromString(String level) {
        return Level.valueOf(level.toUpperCase());
    }
}
