package com.knowledge.agent.common;

public final class ValidationConstants {

    public static final String KB_ID_REGEX = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$";
    public static final String KB_ID_MESSAGE =
            "kbId must start with a letter or digit and contain only letters, digits, dot, underscore or dash";

    private ValidationConstants() {
    }
}
