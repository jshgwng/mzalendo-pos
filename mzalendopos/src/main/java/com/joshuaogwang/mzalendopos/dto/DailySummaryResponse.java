package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailySummaryResponse {
    private LocalDate date;
    private int totalTransactions;
    private double totalRevenue;
    private double totalTax;
    private double totalNet;
}
