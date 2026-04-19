package com.furniro.InventoryService.service;

import org.springframework.stereotype.Service;

import com.furniro.InventoryService.database.entity.StockReservation;
import com.furniro.InventoryService.database.repository.stockReservationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReversionService {
    private final stockReservationRepository stockReservationRepository;


    public Boolean ReservationStock(String sku, Integer quantity) {
        return true;
    }
}
