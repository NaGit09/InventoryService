package com.furniro.InventoryService.exception;

import com.furniro.InventoryService.utils.InventoryErrorCode;

import lombok.Getter;

@Getter
public class InventoryException extends BaseException {
    private final InventoryErrorCode errorCode;

    public InventoryException(InventoryErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
