package com.furniro.InventoryService.database.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.furniro.InventoryService.utils.TransactionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "StockTransaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionID;

    private String sku;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Integer quantity;

    private String referenceID; 

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

