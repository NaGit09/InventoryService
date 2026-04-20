package com.furniro.InventoryService.database.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.furniro.InventoryService.database.entity.Stock;

public interface StockRepository extends JpaRepository<Stock, Integer> {
    Optional<Stock> findByVariantID(Integer variantID);

    Optional<Stock> findBySku(String sku);

    Page<Stock> findAll(Pageable pageable);
    
    @Query("SELECT s FROM Stock s WHERE s.availableQuantity < s.lowStockThreshold")
    Page<Stock> listStockLowThreshold(Pageable pageable);

    @Query("UPDATE Stock s SET s.totalQuantity = s.totalQuantity + :quantity WHERE s.sku = :sku")
    void updateStockBySku(String sku, int quantity);
}
