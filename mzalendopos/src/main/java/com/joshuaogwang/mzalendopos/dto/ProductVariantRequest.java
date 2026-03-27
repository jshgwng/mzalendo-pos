package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductVariantRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String variantName;

    private String sku;

    private String barcode;

    @Min(0)
    private double sellingPrice;

    @Min(0)
    private double costPrice;

    @Min(0)
    private int stockLevel;
}
