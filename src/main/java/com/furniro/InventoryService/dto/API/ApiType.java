package com.furniro.InventoryService.dto.API;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiType<T> extends AType {
    private T data;

    public ApiType(int code, String message, T data) {
        super(code, message);
        this.data = data;
    }

    public static <T> ApiType<T> success(T data) {
        return ApiType.<T>builder()
                .code(200)
                .message("OK")
                .data(data)
                .build();
    }
}