package com.furniro.InventoryService.utils;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InventoryErrorCode {

    SYSTEM_ERROR(500, "System error", HttpStatus.INTERNAL_SERVER_ERROR),

    STOCK_NOT_FOUND(404, "Stock not found", HttpStatus.NOT_FOUND),

    WAREHOUSE_NOT_FOUND(404, "Warehouse not found", HttpStatus.NOT_FOUND),
            
    WAREHOUSE_NOT_ENOUGH_STOCK(400, "Warehouse not enough stock", HttpStatus.BAD_REQUEST),
            
    WAREHOUSE_ALREADY_EXIST(400, "Warehouse already exist", HttpStatus.BAD_REQUEST),

    INVENTORY_NOT_FOUND(404, "Inventory not found", HttpStatus.NOT_FOUND),
    INVALID_PAGE_SIZE(404, "Invalid page size", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    InventoryErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}