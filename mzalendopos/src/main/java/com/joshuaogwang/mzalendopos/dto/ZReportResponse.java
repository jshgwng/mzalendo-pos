package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZReportResponse {

    private Long shiftId;
    private String cashierName;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    private double openingCash;
    private double closingCash;
    private double expectedCash;
    private double cashVariance;

    private int totalTransactions;
    private double totalRevenue;
    private double totalTax;
    private double totalDiscount;

    private double cashSales;
    private double cardSales;
    private double mobileMoneySales;
    private double creditSales;

    private double totalReturns;
    private double totalExpenses;
    private double netCash;

    private List<ExpenseSummary> expenses;
    private String notes;

    @Data
    @Builder
    public static class ExpenseSummary {
        private ExpenseCategory category;
        private double totalAmount;
    }
}
