package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaleItemRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    /** Optional — set when selling a specific variant (size, colour, etc.) */
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
