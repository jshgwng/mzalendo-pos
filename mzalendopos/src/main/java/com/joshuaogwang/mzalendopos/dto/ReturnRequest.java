package com.joshuaogwang.mzalendopos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnRequest {
    @NotNull(message = "Original sale ID is required")
    private Long originalSaleId;

    @NotEmpty(message = "At least one item must be returned")
    @Valid
    private List<ReturnItemRequest> items;

    private String reason;

    @Data
    public static class ReturnItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
