package com.furniro.InventoryService.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.furniro.InventoryService.database.entity.Stock;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, Integer> {
    Optional<Stock> findByVariantID(Integer variantID);

    Optional<Stock> findBySku(String sku);

    Page<Stock> findAll(Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.availableQuantity < s.lowStockThreshold")
    Page<Stock> listStockLowThreshold(Pageable pageable);

    @Query("UPDATE Stock s SET s.totalQuantity = s.totalQuantity + :quantity WHERE s.sku = :sku")
    void updateStockBySku(String sku, int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.sku = :sku")
    Optional<Stock> findBySkuForUpdate(@Param("sku") String sku);

    interface StockSums {
        Integer getTotalAvailable();
        Integer getTotalReserved();
    }

    @Query("SELECT COALESCE(SUM(s.availableQuantity), 0) as totalAvailable, COALESCE(SUM(s.reservedQuantity), 0) as totalReserved FROM Stock s")
    StockSums getStockSums();

    @Query("SELECT s FROM Stock s WHERE s.availableQuantity < s.lowStockThreshold")
    List<Stock> findLowStock();

}
