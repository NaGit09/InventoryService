package com.furniro.InventoryService.utils;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InventoryErrorCode {

    INVENTORY_NOT_FOUND(404, "Inventory not found", HttpStatus.NOT_FOUND),

    INVALID_PAGE_SIZE(404, "Invalid page size", HttpStatus.BAD_REQUEST);

    private final int code; // Business error code (dùng trong response JSON)
    private final String message; // Message trả về client
    private final HttpStatus httpStatus; // HTTP status code thực tế

    InventoryErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}