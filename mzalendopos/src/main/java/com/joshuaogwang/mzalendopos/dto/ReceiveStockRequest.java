package com.joshuaogwang.mzalendopos.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReceiveStockRequest {

    @NotEmpty(message = "Receive items list must not be empty")
    private List<ReceiveItemRequest> items;

    @Data
    public static class ReceiveItemRequest {

        @NotNull(message = "Purchase order item ID is required")
        private Long purchaseOrderItemId;

        @Min(value = 1, message = "Received quantity must be at least 1")
        private int receivedQuantity;
    }
}
