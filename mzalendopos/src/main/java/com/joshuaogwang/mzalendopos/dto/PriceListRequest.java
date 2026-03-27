package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import com.joshuaogwang.mzalendopos.entity.PriceListType;

@Data
public class PriceListRequest {

    @NotBlank
    private String name;

    @NotNull
    private PriceListType type;
}
