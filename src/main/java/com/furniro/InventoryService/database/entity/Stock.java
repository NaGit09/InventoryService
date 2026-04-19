package com.furniro.InventoryService.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stockID;

    @Column(nullable = false, unique = true)
    private Integer variantID;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false, columnDefinition = "int DEFAULT 0")
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(columnDefinition = "int DEFAULT 5")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID")
    private Warehouse warehouse;
    
    @Column(nullable = false, columnDefinition = "int DEFAULT 0")
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(nullable = false, columnDefinition = "int DEFAULT 0")
    @Builder.Default
    private Integer availableQuantity = 0;



    @UpdateTimestamp
    private LocalDateTime updatedAt;
}