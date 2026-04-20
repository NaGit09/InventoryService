package com.furniro.InventoryService.service.kafka;

import com.furniro.InventoryService.dto.req.StockItem;
import com.furniro.InventoryService.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "order.created", groupId = "inventory")
    public void onOrderCreated(Map<String, Object> message) {
        try {
            log.info("Received order.created: {}", message);

            Integer orderId = (Integer) message.get("orderID");
            
            List<StockItem> items = objectMapper.convertValue(message.get("items"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockItem.class));

            reservationService.handleOrderCreated(orderId, items);

            kafkaTemplate.send("inventory.reserved", Map.of("orderID", orderId, "status", "SUCCESS"));

        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", message.get("orderID"), e);
            kafkaTemplate.send("inventory.reserved", Map.of("orderID", message.get("orderID"), "status", "FAILED"));
        }
    }

    @KafkaListener(topics = "payment.completed", groupId = "inventory")
    public void onPaymentCompleted(Map<String, Object> message) {
        try {
            Integer orderId = (Integer) message.get("orderID");
            log.info("Payment completed for order: {}. Committing stock...", orderId);

            reservationService.handlePaymentSuccess(orderId);
        } catch (Exception e) {
            log.error("Error committing stock for order: {}", message.get("orderID"), e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory")
    public void onOrderCancelled(Map<String, Object> message) {
        try {
            Integer orderId = (Integer) message.get("orderID");
            log.info("Order cancelled/Payment failed: {}. Releasing stock...", orderId);

            reservationService.handleOrderCancelled(orderId);
        } catch (Exception e) {
            log.error("Error releasing stock for order: {}", message.get("orderID"), e);
        }
    }

}