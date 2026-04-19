package com.furniro.InventoryService.dto.res;

import java.util.List;

import com.furniro.InventoryService.database.entity.Stock;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockStatistic {
    private Integer totalAvailableStock;
    private Integer totalReservedStock;
    private Integer totalStock;
    private List<Stock> lowStock;
}
