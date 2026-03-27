package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShiftCloseRequest {
    @NotNull(message = "Closing cash is required")
    @Min(value = 0, message = "Closing cash must be non-negative")
    private Double closingCash;

    private String notes;
}
