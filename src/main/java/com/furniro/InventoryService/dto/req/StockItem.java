package com.furniro.InventoryService.dto.req;

import lombok.Data;

@Data
public class StockItem {
    
    private String sku;

    private Integer quantity;
    
    private Integer variantId;
}
