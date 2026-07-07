package com.furniro.InventoryService.service;

import java.util.List;

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
import com.furniro.InventoryService.exception.CustomException;
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
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. return response
        return ResponseEntity.ok(ApiType.success(stock.getAvailableQuantity()));
    }

    // get total stock
    public ResponseEntity<AType> getStatistics() {
        StockRepository.StockSums sums = stockRepository.getStockSums();
        Integer totalAvailable = sums != null && sums.getTotalAvailable() != null ? sums.getTotalAvailable() : 0;
        Integer totalReserved = sums != null && sums.getTotalReserved() != null ? sums.getTotalReserved() : 0;

        List<Stock> lowStock = stockRepository.findLowStock();

        StockStatistic stockStatistic = StockStatistic.builder()
                .totalAvailableStock(totalAvailable)
                .totalReservedStock(totalReserved)
                .totalStock(totalAvailable + totalReserved)
                .lowStock(lowStock)
                .build();

        return ResponseEntity.ok(ApiType.success(stockStatistic));
    }

    // get all stock
    public ResponseEntity<AType> getAllStock(
            int page,
            int size,
            String sortBy) {
        // 1. check page size
        if (page < 0 || size <= 0) {
            throw new CustomException(InventoryErrorCode.INVALID_PAGE_SIZE);
        }

        // 2. create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // 3. find all stock
        Page<Stock> pagenation = stockRepository.findAll(pageable);

        // 4. return response
        return ResponseEntity.ok(ApiType.success(pagenation));
    }

    // check stock low
    public ResponseEntity<AType> checkLowStock(
            int page,
            int size,
            String sortBy) {
        // 1. check page size
        if (page < 0 || size <= 0) {
            throw new CustomException(InventoryErrorCode.INVALID_PAGE_SIZE);
        }

        // 2. create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // 3. find all stock
        Page<Stock> pagenation = stockRepository.listStockLowThreshold(pageable);

        // 4. return response
        return ResponseEntity.ok(ApiType.success(pagenation));
    }

}
