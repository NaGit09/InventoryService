package com.furniro.InventoryService.database.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furniro.InventoryService.database.entity.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    public Optional<Warehouse> findById(Integer id);
}