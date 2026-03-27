package com.joshuaogwang.mzalendopos.dto.accounting;

import java.time.LocalDateTime;
import java.util.List;

import com.joshuaogwang.mzalendopos.entity.AccountingEventType;
import lombok.Builder;
import lombok.Data;

/**
 * Normalized invoice/credit-note payload sent to every accounting provider.
 * Each provider's adapter translates this into its own API format.
 */
@Data
@Builder
public class AccountingInvoiceRequest {

    private AccountingEventType eventType;

    /** POS sale or return reference number */
    private String referenceNumber;

    private LocalDateTime date;

    /** ISO-4217 currency code */
    private String currency;

    private AccountingContact contact;

    private List<AccountingLineItem> lineItems;

    private double subtotal;
    private double taxAmount;
    private double discountAmount;
    private double totalAmount;

    /** Human-readable payment method label */
    private String paymentMethod;

    /**
     * For RETURN_PROCESSED: the external invoice ID / reference from the
     * original sale sync so the credit note can be linked.
     */
    private String originalExternalReference;

    private String notes;
}
