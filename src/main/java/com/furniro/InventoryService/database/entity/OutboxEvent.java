package com.furniro.InventoryService.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String aggregateId; // Ví dụ: orderId
    private String topic; // inventory.reserved
    private String payload; // JSON String của response
    private String status; // PENDING, PROCESSED, FAILED
    private LocalDateTime createdAt = LocalDateTime.now();
}