package com.furniro.InventoryService.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.Warehouse;
import com.furniro.InventoryService.database.repository.StockRepository;
import com.furniro.InventoryService.database.repository.WarehouseRepository;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.dto.res.StockStatistic;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {
    private StockRepository stockRepository;
    private WarehouseRepository warehouseRepository;

    // ==== CRUD ====
    @Transactional
    public Stock createStock(StockReq req) {
        // 1. find warehouse
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId()).get();

        // 2. create stock
        Stock stock = Stock.builder()
                .variantID(req.getVariantId())
                .sku(req.getSku())
                .warehouse(warehouse)
                .totalQuantity(req.getTotalQuantity())
                .build();

        return stockRepository.save(stock);
    }

    @Transactional
    public Stock updateStock(StockReq req) {
        // 1. find stock
        Stock stock = stockRepository.findById(req.getStockId()).get();

        // 2. update stock
        if (req.getType().toUpperCase().equals("IN")) {
            stock.setTotalQuantity(stock.getTotalQuantity() + req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() + req.getQuantity());

        } else if (req.getType().toUpperCase().equals("OUT")) {
            stock.setTotalQuantity(stock.getTotalQuantity() - req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() - req.getQuantity());
        }

        stockRepository.save(stock);

        return stock;
    }

    @Transactional
    public Boolean deleteStock(Integer stockId) {
        // 1. find stock
        Stock stock = stockRepository.findById(stockId).get();

        // 2. check if stock is available
        if (stock.getAvailableQuantity() > 0) {
            return false;
        }

        // 3. delete stock
        stockRepository.deleteById(stockId);
        return true;
    }

    // ==== STATISTIC ====
    // get stock
    public Integer getAvailableStock(String sku) {
        return stockRepository.findBySku(sku).get().getAvailableQuantity();
    }

    // get total stock
    public StockStatistic getTotalStock() {
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

        return StockStatistic.builder()
                .totalAvailableStock(totalAvailableStock)
                .totalReservedStock(totalReservedStock)
                .totalStock(totalAvailableStock + totalReservedStock)
                .lowStock(lowStock)
                .build();
    }

    // get all stock
    public List<Stock> getAllStock() {
        return stockRepository.findAll();
    }

    // check stock low
    public List<Stock> checkLowStock() {
        return stockRepository.findAll().stream()
                .filter(stock -> stock.getAvailableQuantity() < stock.getLowStockThreshold())
                .collect(Collectors.toList());
    }


    // ==== KAFKA EVENT ====
    // kafka event order.completed
    @Transactional
    public Boolean deductStock(Integer stockId, int quantity) {
        Stock stock = stockRepository.findById(stockId).get();
        if (stock.getAvailableQuantity() >= quantity) {
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
            stockRepository.save(stock);
            return true;
        } else {
            return false;
        }
    }

    // kafka event order.cancelled
    @Transactional
    public Boolean releaseStock(Integer stockId, int quantity) {
        Stock stock = stockRepository.findById(stockId).get();
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
    public Stock reserveStock(Integer stockId, int quantity) {
        // 1. find stock
        Stock stock = stockRepository.findById(stockId).get();

        // 2. update stock
        stock.setAvailableQuantity(stock.getAvailableQuantity() - quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

        stockRepository.save(stock);

        return stock;
    }
}
