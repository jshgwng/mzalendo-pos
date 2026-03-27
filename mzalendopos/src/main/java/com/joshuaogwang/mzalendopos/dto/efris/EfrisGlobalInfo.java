package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Global metadata required by the URA EFRIS API envelope.
 * Reference: URA EFRIS Interface Specification v1.1
 */
@Data
@Builder
public class EfrisGlobalInfo {

    /** Application ID — "AP04" for POS applications */
    @JsonProperty("appId")
    private String appId;

    /** Interface version */
    @JsonProperty("version")
    private String version;

    /** Unique exchange ID per request */
    @JsonProperty("dataExchangeId")
    private String dataExchangeId;

    /** T109 = invoice upload */
    @JsonProperty("interfaceCode")
    private String interfaceCode;

    /** "TP" = taxpayer request */
    @JsonProperty("requestCode")
    private String requestCode;

    /** Request timestamp "yyyy-MM-dd HH:mm:ss" */
    @JsonProperty("requestTime")
    private String requestTime;

    /** "BI" = business intelligence response */
    @JsonProperty("responseCode")
    private String responseCode;

    /** Device/VSCU serial number registered with URA */
    @JsonProperty("userName")
    private String userName;

    /** MAC address of the device */
    @JsonProperty("deviceMAC")
    private String deviceMAC;

    /** Device serial number */
    @JsonProperty("deviceNo")
    private String deviceNo;

    /** Taxpayer TIN (Uganda Tax Identification Number) */
    @JsonProperty("tin")
    private String tin;

    /** Branch Reference Number (empty if head office) */
    @JsonProperty("brn")
    private String brn;

    /** "1" = taxpayer */
    @JsonProperty("taxpayerID")
    private String taxpayerID;

    /** Business GPS longitude */
    @JsonProperty("longitude")
    private String longitude;

    /** Business GPS latitude */
    @JsonProperty("latitude")
    private String latitude;

    /** "0" = VSCU/OSCU device */
    @JsonProperty("agentType")
    private String agentType;
}
