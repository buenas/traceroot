package com.traceroot.platform.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Environment {
    DEV, TEST, PROD, STAGING;

    @JsonCreator
    public static Environment fromString(String env) {
        if (env == null) {
            throw new IllegalArgumentException("Environment value cannot be null");
        }
        return Environment.valueOf(env.toUpperCase());
    }
}