package com.furniro.InventoryService.database.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Warehouse")
@Getter
@Setter
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer warehouseID;

    private String name;
    
    private String location;
    
    private Boolean isDefault = false;
}