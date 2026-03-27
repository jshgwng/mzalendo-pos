package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerAccountRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @Min(value = 0, message = "Credit limit must be non-negative")
    private double creditLimit;
}
