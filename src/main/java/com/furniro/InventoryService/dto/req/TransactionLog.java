package com.furniro.InventoryService.dto.req;

import com.furniro.InventoryService.utils.TransactionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionLog {
    String sku;
    Integer quantity;
    TransactionType type;
    String referenceID;
    String note;
}
