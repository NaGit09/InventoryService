package com.furniro.InventoryService.utils;


import lombok.Getter;

@Getter
public enum InventoryErrorCode {

    SYSTEM_ERROR(500, "System error"),

    STOCK_NOT_FOUND(404, "Stock not found"),

    WAREHOUSE_NOT_FOUND(404, "Warehouse not found"),
            
    WAREHOUSE_NOT_ENOUGH_STOCK(400, "Warehouse not enough stock"),
            
    WAREHOUSE_ALREADY_EXIST(400, "Warehouse already exist"),

    INVENTORY_NOT_FOUND(404, "Inventory not found"),
            
    INVALID_PAGE_SIZE(404, "Invalid page size"),

    INVALID_RELEASE_QUANTITY(400, "Invalid release quantity"),
            
    INVALID_INPUT(400, "Invalid input");

    private final int code;
    private final String message;

    InventoryErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}