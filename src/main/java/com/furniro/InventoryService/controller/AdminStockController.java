package com.furniro.InventoryService.controller;

import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.service.StatisticService;
import com.furniro.InventoryService.service.StockService;
import com.furniro.InventoryService.service.StockTransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Stock", description = "Quản lý kho dành cho Admin")
@RestController
@RequestMapping("/admin/stock")
@RequiredArgsConstructor
@Validated
public class AdminStockController {

    private final StockService stockService;
    private final StatisticService statisticService;
    private final StockTransactionService stockTransactionService;

    @PostMapping("/create")
    public ResponseEntity<AType> createStock
    (@Valid @RequestBody StockReq req) {
        return stockService.createStock(req);
    }

    @PutMapping("/update")
    public ResponseEntity<AType> updateStock
    (@Valid @RequestBody StockReq req) {
        return stockService.updateStock(req);
    }

    @DeleteMapping("/delete/{stockId}")
    public ResponseEntity<AType> deleteStock(@PathVariable Integer stockId) {
        return stockService.deleteStock(stockId);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<AType> getStockBySku(@PathVariable String sku) {
        return stockService.getStockBySku(sku);
    }

    @GetMapping("/all")
    public ResponseEntity<AType> getAllStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockID") String sortBy) {
        return statisticService.getAllStock(page, size, sortBy);
    }

    @GetMapping("/statistic")
    public ResponseEntity<AType> getStatistics() {
        return statisticService.getStatistics();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<AType> checkLowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockID") String sortBy) {
        return statisticService.checkLowStock(page, size, sortBy);
    }

    @GetMapping("/transactions")
    public ResponseEntity<AType> getStockLogs(
            @RequestParam(required = false) String sku,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return stockTransactionService.getAllTransactions(sku, page, size);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStockCsv() {
        return stockService.exportStockToCsv();
    }

    @PostMapping("/import")
    public ResponseEntity<AType> importStockCsv(@RequestParam("file") MultipartFile file) {
        return stockService.importStockFromCsv(file);
    }
}