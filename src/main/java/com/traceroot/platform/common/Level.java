package com.traceroot.platform.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Level {
    INFO, WARN, ERROR, DEBUG;

    @JsonCreator
    public static Level fromString(String level) {
        return Level.valueOf(level.toUpperCase());
    }
}
