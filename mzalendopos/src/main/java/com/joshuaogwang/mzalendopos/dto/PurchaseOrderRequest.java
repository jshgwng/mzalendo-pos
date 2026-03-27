package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseOrderRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotEmpty(message = "Order must have at least one item")
    private List<PurchaseOrderItemRequest> items;

    private LocalDateTime expectedDelivery;

    private String notes;

    @Data
    public static class PurchaseOrderItemRequest {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @Min(value = 1, message = "Ordered quantity must be at least 1")
        private int orderedQuantity;

        @Min(value = 0, message = "Unit cost must be non-negative")
        private double unitCost;
    }
}
