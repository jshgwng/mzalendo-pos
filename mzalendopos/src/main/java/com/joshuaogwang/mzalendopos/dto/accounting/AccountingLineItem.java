package com.joshuaogwang.mzalendopos.dto.accounting;

import lombok.Builder;
import lombok.Data;

/**
 * Normalized line item for any accounting provider.
 */
@Data
@Builder
public class AccountingLineItem {
    private String description;
    private String itemCode;
    private int quantity;
    private double unitPrice;
    private double taxRate;
    private double taxAmount;
    private double lineTotal;
}
