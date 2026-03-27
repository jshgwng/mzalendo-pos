package com.joshuaogwang.mzalendopos.dto.accounting;

import java.time.LocalDateTime;

import com.joshuaogwang.mzalendopos.entity.AdjustmentType;
import lombok.Builder;
import lombok.Data;

/**
 * Normalized inventory adjustment payload for accounting tools
 * that support inventory management (e.g. QuickBooks, Zoho Books).
 */
@Data
@Builder
public class AccountingStockAdjustmentRequest {
    private String itemCode;
    private String itemName;
    private AdjustmentType adjustmentType;
    private int quantityChange;
    private double unitCost;
    private LocalDateTime date;
    private String reason;
    private String adjustedBy;
}
