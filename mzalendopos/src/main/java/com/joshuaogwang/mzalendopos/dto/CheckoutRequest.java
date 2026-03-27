package com.joshuaogwang.mzalendopos.dto;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CheckoutRequest {

    /**
     * One or more payment splits to cover the sale total.
     * For a single-method payment, provide exactly one entry.
     * For split payment (e.g. cash + mobile money), provide two entries.
     */
    @NotEmpty(message = "At least one payment is required")
    private List<PaymentSplit> payments;

    private Long customerId;

    @Data
    public static class PaymentSplit {
        @NotNull(message = "Payment method is required")
        private PaymentMethod method;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private Double amount;
    }
}
