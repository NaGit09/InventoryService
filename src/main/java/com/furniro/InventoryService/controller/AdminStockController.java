package com.furniro.InventoryService.controller;

import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.service.StockService;
import com.furniro.InventoryService.service.StockTransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Stock", description = "Quản lý kho dành cho Admin")
@RestController
@RequestMapping("/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;
    // Inject thêm để xem lịch sử
    private final StockTransactionService stockTransactionService;

    // ==== QUẢN LÝ KHO (CRUD) ====
    @PostMapping("/create")
    public ResponseEntity<AType> createStock(@RequestBody StockReq req) {
        return stockService.createStock(req);
    }

    @PutMapping("/update")
    public ResponseEntity<AType> updateStock(@RequestBody StockReq req) {
        // Lưu ý: Trong StockService.updateStock, bạn cần gọi recordTransaction
        return stockService.updateStock(req);
    }

    // ==== THỐNG KÊ & GIÁM SÁT ====
    @GetMapping("/all")
    public ResponseEntity<AType> getAllStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockId") String sortBy) {
        return stockService.getAllStock(page, size, sortBy);
    }

    @GetMapping("/statistic")
    public ResponseEntity<AType> getStatistics() {
        return stockService.getStatistics();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<AType> checkLowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockId") String sortBy) {
        return stockService.checkLowStock(page, size, sortBy);
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