package com.furniro.InventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.service.StockService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Public Stock", description = "API kho hàng cho khách hàng")
@RestController
@RequestMapping("/public/stock")
@RequiredArgsConstructor
public class PublicStockController {

    private final StockService stockService;

    @GetMapping("/available/{sku}")
    public ResponseEntity<AType> getAvailableStock(@PathVariable String sku) {
        return stockService.getAvailableStock(sku);
    }
}