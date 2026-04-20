package com.furniro.InventoryService.dto.req;

import java.util.List;

import lombok.Data;

@Data
public class StockReservationReq {
    private Integer orderId;
    private List<StockItem> stockItems;
}
