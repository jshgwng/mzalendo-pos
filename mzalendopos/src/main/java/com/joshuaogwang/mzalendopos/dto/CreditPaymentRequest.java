package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreditPaymentRequest {

    @Positive(message = "Payment amount must be positive")
    private double amount;

    private String notes;
}
