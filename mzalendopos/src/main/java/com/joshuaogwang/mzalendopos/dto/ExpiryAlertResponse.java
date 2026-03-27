package com.joshuaogwang.mzalendopos.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpiryAlertResponse {

    private Long batchId;
    private Long productId;
    private String productName;
    private String batchNumber;
    private LocalDate expiryDate;
    private long daysUntilExpiry;
    private int remainingQuantity;
}
