package com.furniro.InventoryService.exception;

import com.furniro.InventoryService.dto.API.ErrorType;
import com.furniro.InventoryService.utils.InventoryErrorCode;

public class CustomException extends RuntimeException {
    
    private ErrorType errorCode;

    public CustomException(ErrorType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(InventoryErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = new ErrorType(errorCode.getCode(), errorCode.getMessage());
    }

    public ErrorType getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorType errorCode) {
        this.errorCode = errorCode;
    }
}