package com.furniro.InventoryService.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furniro.InventoryService.database.entity.OutboxEvent;
import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.StockReservation;
import com.furniro.InventoryService.database.repository.OutboxRepository;
import com.furniro.InventoryService.database.repository.ReservationRepository;
import com.furniro.InventoryService.dto.req.StockItem;
import com.furniro.InventoryService.utils.ReservationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final StockService stockService;
    private final ReservationRepository reservationRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveToOutbox(Integer orderId, String status) throws JsonProcessingException {

        Map<String, Object> response = Map.of("orderID", orderId, "status", status);

        OutboxEvent event = new OutboxEvent();

        event.setAggregateId(String.valueOf(orderId));

        event.setTopic("inventory.reserved");

        event.setPayload(objectMapper.writeValueAsString(response));

        event.setStatus("PENDING");

        outboxRepository.save(event);
    }

    public void saveExpiryToOutbox(Integer orderId, String reason) throws JsonProcessingException {

        Map<String, Object> response = Map.of("orderID", orderId, "reason", reason);

        OutboxEvent event = new OutboxEvent();

        event.setAggregateId(String.valueOf(orderId));

        event.setTopic("inventory.reservation-expired");

        event.setPayload(objectMapper.writeValueAsString(response));

        event.setStatus("PENDING");

        outboxRepository.save(event);
    }

    @Transactional
    public void handleOrderCreated(Integer orderId, List<StockItem> items) {

        log.info("Processing reservation for order: {}", orderId);

        for (StockItem item : items) {
            // 1. Update available quantity and reserved quantity in Stock
            Stock stock = stockService.reserveStock(
                    item.getSku(),
                    item.getQuantity(),
                    orderId.toString());

            // 2. Create temporary reservation record
            StockReservation reservation = StockReservation.builder()
                    .orderID(orderId)
                    .sku(stock.getSku())
                    .quantity(item.getQuantity())
                    .expiryTime(LocalDateTime.now().plusMinutes(30))
                    .status(ReservationStatus.PENDING).build();

            reservationRepository.save(reservation);
        }

        try {
            saveToOutbox(orderId, "CREATED");
        } catch (JsonProcessingException e) {
            log.error("Failed to save CREATED state to outbox", e);
            throw new RuntimeException("Failed to save CREATED state to outbox", e);
        }
    }

    @Transactional
    public void handlePaymentSuccess(Integer orderId) {

        List<StockReservation> reservations = reservationRepository
                .findByOrderIDAndStatus(orderId, ReservationStatus.PENDING);

        for (StockReservation res : reservations) {

            // 1. Deduct Total and Reserved in Stock
            stockService.deductStock(
                    res.getSku(),
                    res.getQuantity(),
                    orderId.toString());

            // 2. Update reservation status
            res.setStatus(ReservationStatus.COMPLETED);

            reservationRepository.save(res);
        }
    }

    @Transactional
    public void handleOrderCancelled(Integer orderId) {

        List<StockReservation> reservations = reservationRepository
                .findByOrderIDAndStatus(orderId, ReservationStatus.PENDING);

        for (StockReservation res : reservations) {
            // 1. Return Reserved to Available
            stockService.releaseStock(res.getSku(),
                    res.getQuantity(),
                    orderId.toString());

            // 2. Update reservation status
            res.setStatus(ReservationStatus.CANCELLED);

            reservationRepository.save(res);
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        expireStaleReservations();
    }

    @Transactional
    public List<Integer> expireStaleReservations() {

        LocalDateTime now = LocalDateTime.now();

        List<StockReservation> expired = reservationRepository
                .findAllByStatusAndExpiryTimeBefore(ReservationStatus.PENDING, now);

        if (!expired.isEmpty()) {
            log.info("Cleaning up {} expired reservations", expired.size());

            for (StockReservation res : expired) {
                stockService.releaseStock(
                        res.getSku(),
                        res.getQuantity(),
                        "EXPIRY-" + res.getOrderID());

                res.setStatus(ReservationStatus.EXPIRED);

                reservationRepository.save(res);

                try {
                    saveExpiryToOutbox(res.getOrderID(), "Payment timeout");
                } catch (JsonProcessingException e) {
                    log.error("Failed to save EXPIRED state to outbox for order: {}", res.getOrderID(), e);
                    throw new RuntimeException("Failed to save EXPIRED state to outbox", e);
                }
            }
        }

        return expired.stream()
                .map(reservation -> reservation.getOrderID())
                .distinct()
                .collect(Collectors.toList());
    }
}