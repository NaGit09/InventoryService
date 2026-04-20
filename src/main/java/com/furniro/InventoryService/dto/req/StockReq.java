package com.furniro.InventoryService.dto.req;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReq {

    // 4 field for update stock
    private Integer stockId;

    private String sku;

    private String type;

    private Integer quantity;

    private Integer variantId;

    private Integer warehouseId;

    private Integer totalQuantity;

    private Integer lowStockThreshold;

}
