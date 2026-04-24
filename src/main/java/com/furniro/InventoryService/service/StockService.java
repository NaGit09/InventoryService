package com.furniro.InventoryService.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.Warehouse;
import com.furniro.InventoryService.database.repository.StockRepository;
import com.furniro.InventoryService.database.repository.WarehouseRepository;
import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ApiType;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.exception.InventoryException;
import com.furniro.InventoryService.utils.InventoryErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;

    // ==== CRUD ====
    @Transactional
    public ResponseEntity<AType> createStock(StockReq req) {

        // 1. find warehouse
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));

        // 2. create stock
        Stock stock = Stock.builder()
                .variantID(req.getVariantId())
                .sku(req.getSku())
                .warehouse(warehouse)
                .totalQuantity(req.getTotalQuantity())
                .build();

        stockRepository.save(stock);

        // 3. return response
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Add new stock succeessfully!")
                .data(stock)
                .build());
    }

    @Transactional
    public ResponseEntity<AType> updateStock(StockReq req) {
        // 1. find stock
        Stock stock = stockRepository.findById(req.getStockId())
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. update stock
        if (req.getType().toUpperCase().equals("IN")) {
            stock.setTotalQuantity(stock.getTotalQuantity() + req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() + req.getQuantity());

        } else if (req.getType().toUpperCase().equals("OUT")) {
            stock.setTotalQuantity(stock.getTotalQuantity() - req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() - req.getQuantity());
        }

        stockRepository.save(stock);

        // 3. return response
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Update stock succeessfully!")
                .data(stock)
                .build());
    }

    @Transactional
    public ResponseEntity<AType> deleteStock(Integer stockId) {
        // 1. find stock
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. check if stock is available
        if (stock.getAvailableQuantity() > 0) {
            throw new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
        }

        // 3. delete stock
        stockRepository.deleteById(stockId);
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Delete stock succeessfully!")
                .build());
    }


    // ==== KAFKA EVENT ====
    // kafka event order.completed
    @Transactional
    public Boolean deductStock(String sku, Integer quantity) {
        Stock stock = stockRepository.findBySku(sku).get();
        if (stock.getReservedQuantity() >= quantity) {
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
            stockRepository.save(stock);
            return true;
        } else {
            return false;
        }
    }

    // kafka event order.cancelled
    @Transactional
    public Boolean releaseStock(String sku, Integer quantity) {
        // 1. find stock
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. update stock
        if (stock.getReservedQuantity() >= quantity) {
            stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
            stockRepository.save(stock);
            return true;
        } else {
            return false;
        }
    }

    // kafka event order.pending
    @Transactional
    public Stock reserveStock(String sku, Integer quantity) {
        // 1. find stock
        Stock stock = stockRepository.findBySku(sku).get();

        // 2. update stock
        stock.setAvailableQuantity(stock.getAvailableQuantity() - quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

        stockRepository.save(stock);

        return stock;
    }
}
