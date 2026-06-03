package com.furniro.InventoryService.database.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furniro.InventoryService.database.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Integer> {
    List<OutboxEvent> findByStatus(String status);
}
