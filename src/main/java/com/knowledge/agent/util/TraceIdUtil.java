package com.knowledge.agent.util;

import java.util.UUID;

public final class TraceIdUtil {

    private TraceIdUtil() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
