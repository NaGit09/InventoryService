package com.furniro.InventoryService.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReq {

    private Integer stockId;

    @NotBlank(message = "SKU cannot be blank")
    private String sku;

    private String type;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Variant ID is required")
    private Integer variantId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    @Min(value = 0, message = "Total quantity cannot be negative")
    private Integer totalQuantity;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

}
