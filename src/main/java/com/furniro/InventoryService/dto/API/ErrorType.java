package com.furniro.InventoryService.dto.API;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorType extends AType {

    public ErrorType(int code, String message) {
        super(code, message);
    }

    public static ErrorType badRequest(String message) {
        return ErrorType.builder()
                .code(400)
                .message(message)
                .build();
    }

    public static ErrorType notFound(String message) {
        return ErrorType.builder()
                .code(404)
                .message(message)
                .build();
    }

    public static ErrorType serverError(String message) {
        return ErrorType.builder()
                .code(500)
                .message(message)
                .build();
    }

    public static ErrorType unauthorized(String message) {
        return ErrorType.builder()
                .code(401)
                .message(message)
                .build();
    }

    public static ErrorType forbidden(String message) {
        return ErrorType.builder()
                .code(403)
                .message(message)
                .build();
    }
}