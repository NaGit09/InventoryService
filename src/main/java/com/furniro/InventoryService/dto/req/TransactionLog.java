package com.furniro.InventoryService.dto.req;
import com.furniro.InventoryService.utils.TransactionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionLog {
    @NotBlank(message = "SKU cannot be blank")
    String sku;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity;

    @NotNull(message = "Transaction type is required")
    TransactionType type;

    String referenceID;
    String note;
}
