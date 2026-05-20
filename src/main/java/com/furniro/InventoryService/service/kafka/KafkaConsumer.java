package com.furniro.InventoryService.service.kafka;

import com.furniro.InventoryService.dto.req.StockItem;
import com.furniro.InventoryService.service.ReservationService;
import com.furniro.InventoryService.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final ReservationService reservationService;
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "order.created", groupId = "inventory")
    // ORDER LISTEN THIS EVENT
    public void onOrderCreated(Map<String, Object> message) {
        // 1. init order id
        Integer orderId = null;
        try {
            log.info("Received order.created: {}", message);
            // seperate data from kafka message
            orderId = (Integer) message.get("orderID");

            List<StockItem> items = objectMapper.convertValue(
                    message.get("items"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockItem.class));

            // 2. handle order created
            reservationService.handleOrderCreated(orderId, items);

            // 3. Send response to Order Service
            Map<String, Object> response = Map.of("orderID", orderId, "status", "CREATED");
            kafkaProducer.send("inventory.reserved", response);
            log.info("Sent inventory.reserved (SUCCESS) for order: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", orderId, e);

            Map<String, Object> response = Map.of("orderID", message.get("orderID"), "status", "FAILED");
            kafkaProducer.send("inventory.reserved", response);
            log.warn("Sent inventory.reserved (FAILED) for order: {}", message.get("orderID"));
        }
    }

    @KafkaListener(topics = "payment.completed", groupId = "inventory")
    public void onPaymentCompleted(Map<String, Object> message) {
        // 1. init order id
        Integer orderId = null;
        try {
            log.info("Received payment.completed: {}", message);
            // seperate data from kafka message
            orderId = (Integer) message.get("orderID");

            // 2. handle payment success
            log.info("Payment completed for order: {}. Committing stock...", orderId);

            reservationService.handlePaymentSuccess(orderId);

        } catch (Exception e) {
            log.error("Error committing stock for order: {}", orderId, e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory")
    public void onOrderCancelled(Map<String, Object> message) {
        // 1. init order id
        Integer orderId = null;
        try {
            log.info("Received order.cancelled: {}", message);
            // seperate data from kafka message
            orderId = (Integer) message.get("orderID");

            // 2. handle order cancelled
            log.info("Order cancelled: {}. Releasing stock...", orderId);

            reservationService.handleOrderCancelled(orderId);

        } catch (Exception e) {
            log.error("Error releasing stock for order: {}", orderId, e);
        }
    }

    @KafkaListener(topics = "inventory.restock", groupId = "inventory")
    public void onInventoryRestock(Map<String, Object> message) {
        try {
            log.info("Received inventory.restock: {}", message);
            // 1. seperate data from kafka message
            String sku = (String) message.get("sku");
            Integer quantity = (Integer) message.get("quantity");
            String referenceId = (String) message.get("referenceID");
            String note = (String) message.get("note");

            // 2. handle inventory restock
            stockService.restockBySku(sku, quantity, referenceId, note);
            log.info("Restocked SKU: {} with quantity: {}", sku, quantity);

        } catch (Exception e) {
            log.error("Error processing inventory.restock: {}", message, e);
        }
    }

    @KafkaListener(topics = "inventory.adjustment", groupId = "inventory")
    public void onInventoryAdjustment(Map<String, Object> message) {
        try {
            log.info("Received inventory.adjustment: {}", message);
            // 1. seperate data from kafka message
            String sku = (String) message.get("sku");
            Integer delta = (Integer) message.get("delta");
            String referenceId = (String) message.get("referenceID");
            String note = (String) message.get("note");

            // 2. handle inventory adjustment
            stockService.adjustStock(sku, delta, referenceId, note);
            log.info("Adjusted stock for SKU: {} by delta: {}", sku, delta);

        } catch (Exception e) {
            log.error("Error processing inventory.adjustment: {}", message, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL SCHEDULER
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "reservation.expiry-check", groupId = "inventory")
    // ORDER LISTEN THIS EVENT
    public void onReservationExpiryCheck(Map<String, Object> message) {
        try {
            log.info("Received reservation.expiry-check: {}", message);
            // 1. handle reservation expiry
            List<Integer> expiredOrderIds = reservationService.expireStaleReservations();

            // 2. publish reservation expired event
            for (Integer expiredOrderId : expiredOrderIds) {
                Map<String, Object> event = Map.of(
                        "orderID", expiredOrderId,
                        "reason", "Payment timeout");

                kafkaProducer.send("inventory.reservation-expired", event);
                log.info("Published inventory.reservation-expired for order: {}", expiredOrderId);
            }

        } catch (Exception e) {
            log.error("Error processing reservation.expiry-check", e);
        }
    }
}
