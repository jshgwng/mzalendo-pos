package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShiftOpenRequest {
    @NotNull(message = "Opening cash is required")
    @Min(value = 0, message = "Opening cash must be non-negative")
    private Double openingCash;
}
