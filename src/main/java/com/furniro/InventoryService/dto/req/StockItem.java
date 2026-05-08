package com.furniro.InventoryService.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockItem {
    @NotBlank(message = "SKU is required")
    private String sku;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Variant ID is required")
    private Integer variantId;
}
