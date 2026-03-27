package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PriceListEntryRequest {

    @NotNull
    private Long productId;

    private Long variantId;

    @Positive
    private double price;
}
