package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
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
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private double amountPaid;
    private double changeGiven;

    @Data
    @Builder
    public static class ReceiptItem {
        private String productName;
        private int quantity;
        private double unitPrice;
        private double taxRate;
        private double lineTotal;
    }
}
