package com.furniro.InventoryService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ErrorType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<AType> handleAppExceptions(CustomException ex) {
        AType error = ErrorType.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .build();

        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getErrorCode().getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AType> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        AType error = ErrorType.builder()
                .code(400)
                .message("Validation failed: " + message)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
