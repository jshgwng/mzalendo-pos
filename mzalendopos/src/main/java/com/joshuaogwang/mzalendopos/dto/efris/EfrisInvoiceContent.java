package com.joshuaogwang.mzalendopos.dto.efris;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Inner content of an EFRIS invoice submission (interface code T109).
 * This object is serialised to JSON, AES-256-CBC encrypted, then
 * Base64-encoded before being placed in {@link EfrisRequestData#content}.
 */
@Data
@Builder
public class EfrisInvoiceContent {

    @JsonProperty("sellerDetails")
    private SellerDetails sellerDetails;

    @JsonProperty("basicInformation")
    private BasicInformation basicInformation;

    @JsonProperty("buyerDetails")
    private BuyerDetails buyerDetails;

    @JsonProperty("goodsDetails")
    private List<GoodsDetail> goodsDetails;

    @JsonProperty("taxDetails")
    private List<TaxDetail> taxDetails;

    @JsonProperty("summaryDetails")
    private SummaryDetails summaryDetails;

    @JsonProperty("payWay")
    private List<PayWay> payWay;

    // -------------------------------------------------------------------------

    @Data
    @Builder
    public static class SellerDetails {
        @JsonProperty("tin")
        private String tin;
        @JsonProperty("ninBrn")
        private String ninBrn;
        @JsonProperty("legalName")
        private String legalName;
        @JsonProperty("businessName")
        private String businessName;
        @JsonProperty("address")
        private String address;
        @JsonProperty("mobilePhone")
        private String mobilePhone;
        @JsonProperty("linePhone")
        private String linePhone;
        @JsonProperty("emailAddress")
        private String emailAddress;
        @JsonProperty("placeOfBusiness")
        private String placeOfBusiness;
        @JsonProperty("referenceNo")
        private String referenceNo;
        @JsonProperty("isCheckReferenceNo")
        private String isCheckReferenceNo;
    }

    @Data
    @Builder
    public static class BasicInformation {
        @JsonProperty("invoiceNo")
        private String invoiceNo;
        @JsonProperty("antifakeCode")
        private String antifakeCode;
        @JsonProperty("deviceNo")
        private String deviceNo;
        /** "yyyy-MM-dd HH:mm:ss" */
        @JsonProperty("issuedDate")
        private String issuedDate;
        @JsonProperty("operator")
        private String operator;
        /** Currency code — "UGX" for Uganda shillings */
        @JsonProperty("currency")
        private String currency;
        @JsonProperty("localCurrencyRate")
        private String localCurrencyRate;
        /** "1" = original invoice */
        @JsonProperty("invoiceType")
        private String invoiceType;
        /** "1" = normal; "2" = credit note */
        @JsonProperty("invoiceKind")
        private String invoiceKind;
        /** "103" = POS system */
        @JsonProperty("dataSource")
        private String dataSource;
        @JsonProperty("invoiceIndustryCode")
        private String invoiceIndustryCode;
        /** "0" = single invoice */
        @JsonProperty("isBatch")
        private String isBatch;
    }

    @Data
    @Builder
    public static class BuyerDetails {
        @JsonProperty("buyerTin")
        private String buyerTin;
        @JsonProperty("buyerNinBrn")
        private String buyerNinBrn;
        @JsonProperty("buyerPassportNum")
        private String buyerPassportNum;
        @JsonProperty("buyerLegalName")
        private String buyerLegalName;
        @JsonProperty("buyerBusinessName")
        private String buyerBusinessName;
        @JsonProperty("buyerAddress")
        private String buyerAddress;
        @JsonProperty("buyerEmail")
        private String buyerEmail;
        @JsonProperty("buyerMobilePhone")
        private String buyerMobilePhone;
        @JsonProperty("buyerLinePhone")
        private String buyerLinePhone;
        @JsonProperty("buyerPlaceOfBusiness")
        private String buyerPlaceOfBusiness;
        /** "0" = individual; "1" = business */
        @JsonProperty("buyerType")
        private String buyerType;
        @JsonProperty("buyerCitizenship")
        private String buyerCitizenship;
        @JsonProperty("buyerSector")
        private String buyerSector;
        @JsonProperty("buyerReferenceNo")
        private String buyerReferenceNo;
        @JsonProperty("isCheckReferenceNo")
        private String isCheckReferenceNo;
    }

    @Data
    @Builder
    public static class GoodsDetail {
        @JsonProperty("item")
        private String item;
        @JsonProperty("itemCode")
        private String itemCode;
        /** URA commodity code */
        @JsonProperty("barCode")
        private String barCode;
        @JsonProperty("measure")
        private String measure;
        @JsonProperty("quantity")
        private String quantity;
        @JsonProperty("unitPrice")
        private String unitPrice;
        /** Total before tax */
        @JsonProperty("total")
        private String total;
        @JsonProperty("taxRate")
        private String taxRate;
        @JsonProperty("tax")
        private String tax;
        @JsonProperty("orderNumber")
        private String orderNumber;
        /** "101" = goods */
        @JsonProperty("goodsCategoryId")
        private String goodsCategoryId;
        @JsonProperty("goodsCategoryName")
        private String goodsCategoryName;
        @JsonProperty("serviceType")
        private String serviceType;
        @JsonProperty("discountTaxRate")
        private String discountTaxRate;
        @JsonProperty("discountTotal")
        private String discountTotal;
        @JsonProperty("discountFlag")
        private String discountFlag;
    }

    @Data
    @Builder
    public static class TaxDetail {
        /** "101" = VAT */
        @JsonProperty("taxCategory")
        private String taxCategory;
        @JsonProperty("taxRate")
        private String taxRate;
        @JsonProperty("netAmount")
        private String netAmount;
        @JsonProperty("taxAmount")
        private String taxAmount;
    }

    @Data
    @Builder
    public static class SummaryDetails {
        @JsonProperty("netAmount")
        private String netAmount;
        @JsonProperty("taxAmount")
        private String taxAmount;
        @JsonProperty("grossAmount")
        private String grossAmount;
        @JsonProperty("itemCount")
        private String itemCount;
        /** "0" = normal */
        @JsonProperty("modeCode")
        private String modeCode;
        @JsonProperty("remarks")
        private String remarks;
        @JsonProperty("qrCode")
        private String qrCode;
    }

    @Data
    @Builder
    public static class PayWay {
        /**
         * URA payment mode codes:
         * 101 = Cash, 102 = Credit Card, 103 = EFT/Mobile Money
         */
        @JsonProperty("paymentMode")
        private String paymentMode;
        @JsonProperty("paymentAmount")
        private String paymentAmount;
        @JsonProperty("orderNumber")
        private String orderNumber;
    }
}
