package com.joshuaogwang.mzalendopos.dto;

import com.joshuaogwang.mzalendopos.entity.AdjustmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Adjustment type is required")
    private AdjustmentType adjustmentType;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String reason;
}
