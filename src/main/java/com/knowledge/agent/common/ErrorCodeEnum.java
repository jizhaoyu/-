package com.knowledge.agent.common;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    SUCCESS(200, "ok"),
    BAD_REQUEST(400, "bad request"),
    RESOURCE_NOT_FOUND(404, "resource not found"),
    SYSTEM_ERROR(500, "internal system error"),

    DOCUMENT_NOT_FOUND(4101, "document not found"),
    UNSUPPORTED_FILE_TYPE(4102, "unsupported file type"),
    DOCUMENT_PARSE_FAILED(4103, "document parse failed"),
    DOCUMENT_INDEX_FAILED(4104, "document indexing failed"),

    EMBEDDING_FAILED(4201, "embedding request failed"),
    VECTOR_STORE_FAILED(4202, "vector store request failed"),
    LLM_FAILED(4203, "llm request failed");

    private final int code;
    private final String message;

    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
