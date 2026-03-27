package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductBatchRequest {

    @NotNull
    private Long productId;

    private Long variantId;

    @NotBlank
    private String batchNumber;

    @NotNull
    private LocalDate expiryDate;

    @Min(1)
    private int quantity;

    @Min(0)
    private double unitCost;

    private Long purchaseOrderId;
}
