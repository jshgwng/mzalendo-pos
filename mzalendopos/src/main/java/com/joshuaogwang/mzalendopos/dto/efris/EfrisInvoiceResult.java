package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Decrypted content from a successful T109 EFRIS invoice response.
 * Contains the Fiscal Receipt Number (FRN) and QR code issued by URA.
 */
@Data
public class EfrisInvoiceResult {

    @JsonProperty("basicInformation")
    private BasicInfo basicInformation;

    @Data
    public static class BasicInfo {
        /** Fiscal Receipt Number issued by URA */
        @JsonProperty("antifakeCode")
        private String antifakeCode;

        @JsonProperty("invoiceNo")
        private String invoiceNo;

        @JsonProperty("issuedDate")
        private String issuedDate;
    }

    @JsonProperty("summaryDetails")
    private SummaryInfo summaryDetails;

    @Data
    public static class SummaryInfo {
        /** Base64-encoded QR code image data or verification URL */
        @JsonProperty("qrCode")
        private String qrCode;
    }
}
