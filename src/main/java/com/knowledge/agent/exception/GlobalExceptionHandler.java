package com.knowledge.agent.exception;

import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.common.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Hidden
    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleServiceException(ServiceException ex) {
        log.warn("business error: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @Hidden
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return Result.fail(ErrorCodeEnum.BAD_REQUEST.getCode(), extractValidationMessage(ex.getBindingResult()));
    }

    @Hidden
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        return Result.fail(ErrorCodeEnum.BAD_REQUEST.getCode(), extractValidationMessage(ex.getBindingResult()));
    }

    @Hidden
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCodeEnum.BAD_REQUEST.getMessage());
        return Result.fail(ErrorCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @Hidden
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ErrorCodeEnum.RESOURCE_NOT_FOUND.getCode(), ErrorCodeEnum.RESOURCE_NOT_FOUND.getMessage()));
    }

    @Hidden
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("unexpected error", ex);
        return Result.fail(ErrorCodeEnum.SYSTEM_ERROR.getCode(), ErrorCodeEnum.SYSTEM_ERROR.getMessage());
    }

    private String extractValidationMessage(BindingResult bindingResult) {
        if (bindingResult.getFieldError() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }
        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(ErrorCodeEnum.BAD_REQUEST.getMessage());
    }
}
