package com.joshuaogwang.mzalendopos.dto;

import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount paid is required")
    @Positive(message = "Amount paid must be positive")
    private Double amountPaid;

    private Long customerId;
}
