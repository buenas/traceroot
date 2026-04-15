package com.traceroot.platform.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Environment {
    dev, test, prod, staging;

    @JsonCreator
    public static Environment fromString(String env) {
        return Environment.valueOf(env.toLowerCase());
    }
}
