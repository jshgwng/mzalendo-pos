package com.joshuaogwang.mzalendopos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Accounting tool integration configuration.
 *
 * Each provider has its own nested block. Enable/disable individually.
 *
 * Example (application.properties):
 *
 *   accounting.quickbooks.enabled=true
 *   accounting.quickbooks.client-id=${QB_CLIENT_ID}
 *   accounting.quickbooks.client-secret=${QB_CLIENT_SECRET}
 *   accounting.quickbooks.redirect-uri=https://yourpos.com/api/v1/accounting/quickbooks/callback
 *   accounting.quickbooks.sandbox=true
 *
 *   accounting.xero.enabled=true
 *   accounting.xero.client-id=${XERO_CLIENT_ID}
 *   accounting.xero.client-secret=${XERO_CLIENT_SECRET}
 *   accounting.xero.redirect-uri=https://yourpos.com/api/v1/accounting/xero/callback
 *
 *   accounting.zoho.enabled=false
 *   accounting.zoho.client-id=${ZOHO_CLIENT_ID}
 *   accounting.zoho.client-secret=${ZOHO_CLIENT_SECRET}
 *   accounting.zoho.redirect-uri=https://yourpos.com/api/v1/accounting/zoho/callback
 *   accounting.zoho.region=com   (com | eu | in | au | jp)
 */
@Data
@Component
@ConfigurationProperties(prefix = "accounting")
public class AccountingProperties {

    private QuickBooksConfig quickbooks = new QuickBooksConfig();
    private XeroConfig xero = new XeroConfig();
    private ZohoConfig zoho = new ZohoConfig();

    /** Max retry attempts for failed syncs */
    private int maxRetryAttempts = 3;

    /** Retry scheduler interval in milliseconds (default 5 minutes) */
    private long retryIntervalMs = 300_000;

    // -------------------------------------------------------------------------

    @Data
    public static class QuickBooksConfig {
        private boolean enabled = false;
        private boolean sandbox = true;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        private String authorizationUrl =
                "https://appcenter.intuit.com/connect/oauth2";
        private String tokenUrl =
                "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";
        private String sandboxApiBase =
                "https://sandbox-quickbooks.api.intuit.com/v3/company";
        private String productionApiBase =
                "https://quickbooks.api.intuit.com/v3/company";
        private String revokeUrl =
                "https://developer.api.intuit.com/v2/oauth2/tokens/revoke";

        /** QuickBooks account name for recording income (e.g. "Sales of Product Income") */
        private String incomeAccountName = "Sales of Product Income";

        public String getApiBase() {
            return sandbox ? sandboxApiBase : productionApiBase;
        }
    }

    @Data
    public static class XeroConfig {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        private String authorizationUrl =
                "https://login.xero.com/identity/connect/authorize";
        private String tokenUrl =
                "https://identity.xero.com/connect/token";
        private String connectionsUrl =
                "https://api.xero.com/connections";
        private String apiBase =
                "https://api.xero.com/api.xro/2.0";
        private String revokeUrl =
                "https://identity.xero.com/connect/revocation";

        /** Xero account code for sales income (e.g. "200") */
        private String revenueAccountCode = "200";

        /** Xero account code for tax collected (e.g. "OUTPUT") */
        private String taxAccountCode = "OUTPUT";
    }

    @Data
    public static class ZohoConfig {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        /** Zoho region: com | eu | in | au | jp */
        private String region = "com";

        public String getAuthorizationUrl() {
            return "https://accounts.zoho." + region + "/oauth/v2/auth";
        }

        public String getTokenUrl() {
            return "https://accounts.zoho." + region + "/oauth/v2/token";
        }

        public String getApiBase() {
            return "https://www.zohoapis." + region + "/books/v3";
        }
    }
}
