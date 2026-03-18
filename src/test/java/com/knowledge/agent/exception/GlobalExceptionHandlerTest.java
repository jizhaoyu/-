package com.knowledge.agent.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.agent.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("bind exception without field error should still return bad request")
    void handleBindException_shouldFallbackToGlobalErrorMessage() {
        BindException bindException = new BindException(new Object(), "query");
        bindException.addError(new ObjectError("query", "request is invalid"));

        Result<Void> result = handler.handleBindException(bindException);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("request is invalid");
    }

    @Test
    @DisplayName("missing static resource should return 404 instead of system error")
    void handleNoResourceFoundException_shouldReturnNotFound() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico");

        ResponseEntity<Result<Void>> response = handler.handleNoResourceFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("resource not found");
    }
}
