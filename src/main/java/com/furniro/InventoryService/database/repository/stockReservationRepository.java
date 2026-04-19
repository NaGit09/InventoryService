package com.furniro.InventoryService.database.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furniro.InventoryService.database.entity.StockReservation;

public interface stockReservationRepository extends JpaRepository<StockReservation, Integer> {
    Optional<StockReservation> findBySku(String sku);
}
