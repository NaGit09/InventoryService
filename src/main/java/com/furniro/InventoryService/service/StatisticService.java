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
import com.furniro.InventoryService.database.repository.StockRepository;
import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ApiType;
import com.furniro.InventoryService.dto.res.StockStatistic;
import com.furniro.InventoryService.exception.InventoryException;
import com.furniro.InventoryService.utils.InventoryErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {
    private final StockRepository stockRepository;

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
    public ResponseEntity<AType> getAllStock(
        int page,
        int size,
        String sortBy
    ) {
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
    public ResponseEntity<AType> checkLowStock(
        int page,
        int size,
        String sortBy
    ) {
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

}
