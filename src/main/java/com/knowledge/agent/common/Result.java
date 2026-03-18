package com.knowledge.agent.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unified API response")
public class Result<T> {

    @Schema(description = "Business code")
    private int code;

    @Schema(description = "Message")
    private String message;

    @Schema(description = "Payload")
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ErrorCodeEnum.SUCCESS.getCode())
                .message(ErrorCodeEnum.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> Result<T> fail(ErrorCodeEnum codeEnum) {
        return Result.<T>builder()
                .code(codeEnum.getCode())
                .message(codeEnum.getMessage())
                .build();
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
