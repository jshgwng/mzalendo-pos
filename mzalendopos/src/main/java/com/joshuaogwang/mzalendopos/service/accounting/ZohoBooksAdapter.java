package com.joshuaogwang.mzalendopos.service.accounting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.joshuaogwang.mzalendopos.config.AccountingProperties;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingInvoiceRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingLineItem;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingStockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingSyncResult;
import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingEventType;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSyncStatus;
import com.joshuaogwang.mzalendopos.entity.AdjustmentType;

/**
 * Zoho Books adapter.
 *
 * API reference: https://www.zoho.com/books/api/v3/
 *
 * Implements:
 *  - Invoice creation for completed sales
 *  - Credit Note for returns
 *  - Inventory Adjustment for stock changes (Zoho Books supports inventory)
 *
 * Note: Zoho uses a different OAuth2 flow — the refresh token is long-lived
 * and does not rotate, unlike QuickBooks/Xero.
 */
@Component
public class ZohoBooksAdapter extends BaseOAuth2Adapter {

    private static final DateTimeFormatter ZOHO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AccountingProperties properties;

    @Autowired
    public ZohoBooksAdapter(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public AccountingProviderType getProviderType() {
        return AccountingProviderType.ZOHO_BOOKS;
    }

    // -------------------------------------------------------------------------
    // OAuth2
    // -------------------------------------------------------------------------

    @Override
    public String buildAuthorizationUrl(String state) {
        AccountingProperties.ZohoConfig cfg = properties.getZoho();
        return UriComponentsBuilder.fromHttpUrl(cfg.getAuthorizationUrl())
                .queryParam("client_id", cfg.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", "ZohoBooks.fullaccess.all")
                .queryParam("redirect_uri", cfg.getRedirectUri())
                .queryParam("access_type", "offline")
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public AccountingCredentials exchangeCodeForTokens(String code, AccountingCredentials credentials) {
        AccountingProperties.ZohoConfig cfg = properties.getZoho();
        Map<String, Object> response = exchangeCode(
                cfg.getTokenUrl(), cfg.getClientId(), cfg.getClientSecret(),
                code, cfg.getRedirectUri());
        applyTokenResponse(response, credentials);
        credentials.setActive(true);
        credentials.setConnectedAt(LocalDateTime.now());

        // Fetch organisationId
        fetchOrganisationId(credentials);
        return credentials;
    }

    @Override
    public AccountingCredentials refreshAccessToken(AccountingCredentials credentials) {
        AccountingProperties.ZohoConfig cfg = properties.getZoho();
        // Zoho refresh token grant uses query params, not Basic auth
        String url = UriComponentsBuilder.fromHttpUrl(cfg.getTokenUrl())
                .queryParam("refresh_token", credentials.getRefreshToken())
                .queryParam("client_id", cfg.getClientId())
                .queryParam("client_secret", cfg.getClientSecret())
                .queryParam("grant_type", "refresh_token")
                .toUriString();

        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
        applyTokenResponse(response, credentials);
        return credentials;
    }

    @Override
    public void disconnect(AccountingCredentials credentials) {
        // Zoho revoke endpoint
        try {
            AccountingProperties.ZohoConfig cfg = properties.getZoho();
            String revokeUrl = "https://accounts.zoho." + cfg.getRegion()
                    + "/oauth/v2/token/revoke?token=" + credentials.getRefreshToken();
            restTemplate.postForObject(revokeUrl, null, Void.class);
        } catch (Exception ex) {
            log.warn("Zoho token revoke failed: {}", ex.getMessage());
        }
        credentials.setActive(false);
        credentials.setAccessToken(null);
        credentials.setRefreshToken(null);
    }

    // -------------------------------------------------------------------------
    // Invoice sync
    // -------------------------------------------------------------------------

    @Override
    public AccountingSyncResult syncInvoice(AccountingInvoiceRequest request,
            AccountingCredentials credentials) {
        try {
            if (request.getEventType() == AccountingEventType.RETURN_PROCESSED) {
                return createCreditNote(request, credentials);
            }
            return createInvoice(request, credentials);

        } catch (HttpClientErrorException ex) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage("Zoho HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString())
                    .build();
        } catch (Exception ex) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.RETRY)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult createInvoice(AccountingInvoiceRequest request,
            AccountingCredentials credentials) {

        List<Map<String, Object>> lineItems = new ArrayList<>();
        for (AccountingLineItem item : request.getLineItems()) {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.getDescription());
            line.put("quantity", item.getQuantity());
            line.put("rate", item.getUnitPrice());
            if (item.getTaxRate() > 0) {
                line.put("tax_percentage", item.getTaxRate());
            }
            lineItems.add(line);
        }

        Map<String, Object> invoice = new HashMap<>();
        invoice.put("invoice_number", request.getReferenceNumber());
        invoice.put("date", request.getDate().format(ZOHO_DATE));
        invoice.put("due_date", request.getDate().format(ZOHO_DATE));
        invoice.put("currency_code", request.getCurrency());
        invoice.put("line_items", lineItems);
        invoice.put("notes", request.getPaymentMethod()
                + (request.getNotes() != null ? " — " + request.getNotes() : ""));
        invoice.put("payment_terms", 0);

        if (request.getContact() != null && request.getContact().getName() != null) {
            invoice.put("customer_name", request.getContact().getName());
        }

        if (request.getDiscountAmount() > 0) {
            invoice.put("discount", request.getDiscountAmount());
            invoice.put("is_discount_before_tax", true);
        }

        String url = zohoUrl(credentials, "/invoices");
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(Map.of("JSONString",
                        jsonString(invoice)), zohoHeaders(credentials)), Map.class);

        return extractZohoResult(response, "invoice", "invoice_id", "invoice_number",
                "zoho-books/invoices/");
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult createCreditNote(AccountingInvoiceRequest request,
            AccountingCredentials credentials) {

        List<Map<String, Object>> lineItems = new ArrayList<>();
        for (AccountingLineItem item : request.getLineItems()) {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.getDescription());
            line.put("quantity", item.getQuantity());
            line.put("rate", item.getUnitPrice());
            lineItems.add(line);
        }

        Map<String, Object> creditNote = new HashMap<>();
        creditNote.put("creditnote_number", request.getReferenceNumber());
        creditNote.put("date", request.getDate().format(ZOHO_DATE));
        creditNote.put("currency_code", request.getCurrency());
        creditNote.put("line_items", lineItems);
        creditNote.put("notes", "Return for: " + request.getOriginalExternalReference());

        if (request.getContact() != null && request.getContact().getName() != null) {
            creditNote.put("customer_name", request.getContact().getName());
        }

        String url = zohoUrl(credentials, "/creditnotes");
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(Map.of("JSONString",
                        jsonString(creditNote)), zohoHeaders(credentials)), Map.class);

        return extractZohoResult(response, "creditnote", "creditnote_id",
                "creditnote_number", "zoho-books/creditnotes/");
    }

    // -------------------------------------------------------------------------
    // Stock adjustment
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public AccountingSyncResult syncStockAdjustment(AccountingStockAdjustmentRequest request,
            AccountingCredentials credentials) {
        try {
            Map<String, Object> adjustmentItem = new HashMap<>();
            adjustmentItem.put("name", request.getItemName());
            adjustmentItem.put("quantity_adjusted", Math.abs(request.getQuantityChange()));
            adjustmentItem.put("purchase_rate", request.getUnitCost());

            Map<String, Object> payload = new HashMap<>();
            payload.put("date", request.getDate().format(ZOHO_DATE));
            payload.put("reason", request.getReason() != null ? request.getReason()
                    : request.getAdjustmentType().name());
            payload.put("adjustment_type", toZohoAdjustmentType(request.getAdjustmentType()));
            payload.put("line_items", List.of(adjustmentItem));

            String url = zohoUrl(credentials, "/inventoryadjustments");
            Map<String, Object> response = restTemplate.postForObject(
                    url, new HttpEntity<>(Map.of("JSONString",
                            jsonString(payload)), zohoHeaders(credentials)), Map.class);

            return extractZohoResult(response, "inventory_adjustment",
                    "inventory_adjustment_id", "adjustment_description",
                    "zoho-books/inventory/");

        } catch (Exception ex) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.RETRY)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void fetchOrganisationId(AccountingCredentials credentials) {
        try {
            AccountingProperties.ZohoConfig cfg = properties.getZoho();
            String url = cfg.getApiBase() + "/organizations";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(zohoHeaders(credentials)), Map.class);
            if (response.getBody() != null) {
                List<Map<String, Object>> orgs =
                        (List<Map<String, Object>>) response.getBody().get("organizations");
                if (orgs != null && !orgs.isEmpty()) {
                    credentials.setTenantId((String) orgs.get(0).get("organization_id"));
                    credentials.setOrganisationName((String) orgs.get(0).get("name"));
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch Zoho organisation ID: {}", ex.getMessage());
        }
    }

    private HttpHeaders zohoHeaders(AccountingCredentials credentials) {
        HttpHeaders headers = bearerHeaders(credentials);
        headers.setContentType(null);
        // Zoho uses multipart form for some endpoints; we use JSON body
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        return headers;
    }

    private String zohoUrl(AccountingCredentials credentials, String path) {
        return properties.getZoho().getApiBase() + path
                + "?organization_id=" + credentials.getTenantId();
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult extractZohoResult(Map<String, Object> response,
            String entityKey, String idField, String numberField, String urlPrefix) {
        if (response == null) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("Null Zoho response").build();
        }
        Integer code = (Integer) response.get("code");
        if (code != null && code != 0) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage((String) response.get("message"))
                    .build();
        }
        Map<String, Object> entity = (Map<String, Object>) response.get(entityKey);
        if (entity == null) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("Missing " + entityKey + " in Zoho response").build();
        }
        String id = (String) entity.get(idField);
        String number = entity.containsKey(numberField) ? (String) entity.get(numberField) : id;
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SYNCED)
                .externalReference("ZOHO-" + number)
                .externalUrl("https://books.zoho.com/" + urlPrefix + id)
                .build();
    }

    private String toZohoAdjustmentType(AdjustmentType type) {
        return switch (type) {
            case DAMAGE, THEFT -> "quantity";
            case RESTOCK, OPENING_STOCK, RETURN, CORRECTION -> "quantity";
        };
    }

    private String jsonString(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize Zoho payload", ex);
        }
    }
}
