package com.furniro.InventoryService.database.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.furniro.InventoryService.database.entity.StockTransaction;
import com.furniro.InventoryService.utils.TransactionType;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Integer> {
    @Query("SELECT s FROM StockTransaction s WHERE s.referenceID = :orderId AND s.type = :type")
    Optional<StockTransaction> findByReferenceIDAndType(String orderId, TransactionType type);
    
    Page<StockTransaction> findBySku(String sku, Pageable pageable);
    
    Page<StockTransaction> findAll(Pageable pageable);
}
