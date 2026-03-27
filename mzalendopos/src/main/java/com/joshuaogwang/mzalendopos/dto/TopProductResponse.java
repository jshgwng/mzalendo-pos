package com.joshuaogwang.mzalendopos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductResponse {
    private String productName;
    private long totalQuantitySold;
    private double totalRevenue;
}
