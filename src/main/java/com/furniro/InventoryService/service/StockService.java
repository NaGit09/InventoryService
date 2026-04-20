package com.furniro.InventoryService.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.Warehouse;
import com.furniro.InventoryService.database.repository.StockRepository;
import com.furniro.InventoryService.database.repository.WarehouseRepository;
import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ApiType;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.dto.res.StockStatistic;
import com.furniro.InventoryService.exception.InventoryException;
import com.furniro.InventoryService.utils.InventoryErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;

    // ==== CRUD ====
    @Transactional
    public ResponseEntity<AType> createStock(StockReq req) {

        // 1. find warehouse
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() ->
                        new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));

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
                .orElseThrow(() ->
                        new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

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
                .orElseThrow(() ->
                        new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

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

    // ==== STATISTIC ====
    // get stock
    public ResponseEntity<AType> getAvailableStock(String sku) {
        // 1. find stock
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() ->
                        new InventoryException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. return response
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Get available stock succeessfully!")
                .data(stock.getAvailableQuantity())
                .build());
    }

    // get total stock
    public ResponseEntity<AType> getStatistics() {
        // get all stock
        List<Stock> stocks = stockRepository.findAll();

        // total available stock
        Integer totalAvailableStock = stocks.stream()
                .map(Stock::getAvailableQuantity)
                .reduce(0, Integer::sum);

        // total reserved stock
        Integer totalReservedStock = stocks.stream()
                .map(Stock::getReservedQuantity)
                .reduce(0, Integer::sum);

        // list low stock
        List<Stock> lowStock = stocks.stream()
                .filter(stock -> stock.getAvailableQuantity() < stock.getLowStockThreshold())
                .collect(Collectors.toList());

        StockStatistic stockStatistic = StockStatistic.builder()
                .totalAvailableStock(totalAvailableStock)
                .totalReservedStock(totalReservedStock)
                .totalStock(totalAvailableStock + totalReservedStock)
                .lowStock(lowStock)
                .build();
                
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Get total stock succeessfully!")
                .data(stockStatistic)
                .build());
    }

    // get all stock    
    public ResponseEntity<AType> getAllStock(int page, int size, String sortBy) {
        // 1. check page size
        if (page < 0 || size <= 0) {
            throw new InventoryException(InventoryErrorCode.INVALID_PAGE_SIZE);
        }

        // 2. create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // 3. find all stock
        Page<Stock> pagenation = stockRepository.findAll(pageable);

        // 4. return response
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Get all stock succeessfully!")
                .data(pagenation)
                .build());
    }

    // check stock low
    public ResponseEntity<AType> checkLowStock(int page, int size, String sortBy) {
        // 1. check page size
        if (page < 0 || size <= 0) {
            throw new InventoryException(InventoryErrorCode.INVALID_PAGE_SIZE);
        }

        // 2. create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // 3. find all stock
        Page<Stock> pagenation = stockRepository.listStockLowThreshold(pageable);
        
 
        // 4. return response
        return ResponseEntity.ok(ApiType.builder()
                .code(200)
                .message("Get all stock succeessfully!")
                .data(pagenation)
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
        Stock stock = stockRepository.findBySku(sku).get();
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
