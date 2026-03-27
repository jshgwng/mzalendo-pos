package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.joshuaogwang.mzalendopos.entity.DiscountType;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceiptResponse {
    private String saleNumber;
    private String cashier;
    private String customer;
    private LocalDateTime completedAt;
    private List<ReceiptItem> items;
    private double subtotal;
    private double taxAmount;
    private DiscountType discountType;
    private double discountValue;
    private double discountAmount;
    private double totalAmount;
    private double promotionDiscount;
    /** All payment splits (one entry for single-method, multiple for split payment) */
    private List<ReceiptPayment> payments;
    private double totalAmountPaid;
    private double changeGiven;

    // EFRIS URA fiscal receipt fields (null when EFRIS is disabled or pending)
    private String fiscalReceiptNumber;
    private String efrisQrCode;
    private String efrisAntifakeCode;

    @Data
    @Builder
    public static class ReceiptItem {
        private String productName;
        private String variantName;
        private int quantity;
        private double unitPrice;
        private double taxRate;
        private double lineTotal;
    }

    @Data
    @Builder
    public static class ReceiptPayment {
        private PaymentMethod method;
        private double amount;
        private double changeGiven;
    }
}
