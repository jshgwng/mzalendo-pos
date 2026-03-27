package com.joshuaogwang.mzalendopos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * EFRIS URA integration configuration.
 *
 * Set these via environment variables or application.properties:
 *
 *   efris.enabled=true
 *   efris.sandbox=true          (false for production)
 *   efris.api-url=https://efrisws.ura.go.ug/api/efrisws/ws/taInvoiceRequestLocal
 *   efris.sandbox-url=https://efrisws.ura.go.ug/sandbox/efrisws/ws/taInvoiceRequestLocal
 *   efris.tin=1234567890        (your URA TIN)
 *   efris.device-no=VSCU123456  (VSCU/OSCU serial registered with URA)
 *   efris.device-mac=AA:BB:CC:DD:EE:FF
 *   efris.private-key=<32-char AES key registered with URA>
 *   efris.legal-name=My Business Ltd
 *   efris.business-name=My Trading Name
 *   efris.address=Plot 1, Kampala Road, Kampala
 *   efris.mobile-phone=+256700000000
 *   efris.place-of-business=Kampala
 *   efris.longitude=32.5825
 *   efris.latitude=0.3476
 *   efris.industry-code=101
 */
@Data
@Component
@ConfigurationProperties(prefix = "efris")
public class EfrisProperties {

    /** Enable/disable EFRIS integration entirely */
    private boolean enabled = false;

    /** Use sandbox endpoint (true) or production (false) */
    private boolean sandbox = true;

    private String apiUrl = "https://efrisws.ura.go.ug/api/efrisws/ws/taInvoiceRequestLocal";

    private String sandboxUrl = "https://efrisws.ura.go.ug/sandbox/efrisws/ws/taInvoiceRequestLocal";

    /** Taxpayer Identification Number */
    private String tin = "";

    /** VSCU/OSCU device serial number registered with URA */
    private String deviceNo = "";

    /** Device MAC address */
    private String deviceMac = "";

    /**
     * AES-256 private key (32-character UTF-8 string) provided by URA
     * when registering the VSCU device.
     */
    private String privateKey = "";

    /** Business legal name as registered with URA */
    private String legalName = "";

    /** Trading/business name */
    private String businessName = "";

    /** Physical address */
    private String address = "";

    private String mobilePhone = "";

    private String placeOfBusiness = "";

    /** GPS longitude of the business premises */
    private String longitude = "32.5825";

    /** GPS latitude of the business premises */
    private String latitude = "0.3476";

    /** URA industry code (101 = retail) */
    private String industryCode = "101";

    /** Max submission retry attempts before giving up */
    private int maxRetryAttempts = 3;

    /** HTTP connect/read timeout in milliseconds */
    private int timeoutMs = 10000;

    public String getEffectiveApiUrl() {
        return sandbox ? sandboxUrl : apiUrl;
    }
}
