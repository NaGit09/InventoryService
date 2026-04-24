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

@Tag(name = "Admin Stock", description = "Quản lý kho dành cho Admin")
@RestController
@RequestMapping("/admin/stock")
@RequiredArgsConstructor
@Validated
public class AdminStockController {

    private final StockService stockService;
    private final StatisticService statisticService;
    private final StockTransactionService stockTransactionService;

    // ==== QUẢN LÝ KHO (CRUD) ====
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

    // ==== THỐNG KÊ & GIÁM SÁT ====
    @GetMapping("/all")
    public ResponseEntity<AType> getAllStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockId") String sortBy) {
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
            @RequestParam(defaultValue = "stockId") String sortBy) {
        return statisticService.checkLowStock(page, size, sortBy);
    }

    // ==== NHẬT KÝ BIẾN ĐỘNG (BỔ SUNG) ====
    @GetMapping("/transactions")
    public ResponseEntity<AType> getStockLogs(
            @RequestParam(required = false) String sku,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Gọi hàm tìm kiếm lịch sử từ StockTransactionService
        return stockTransactionService.getAllTransactions(sku, page, size);
    }
}