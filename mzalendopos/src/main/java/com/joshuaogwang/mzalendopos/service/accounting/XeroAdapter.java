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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshuaogwang.mzalendopos.config.AccountingProperties;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingInvoiceRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingLineItem;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingStockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingSyncResult;
import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingEventType;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSyncStatus;

/**
 * Xero accounting adapter.
 *
 * API reference: https://developer.xero.com/documentation/api/accounting/overview
 *
 * Implements:
 *  - ACCREC Invoice (with AUTHORISED status) for completed sales
 *  - Credit Note for returns
 *  - Xero does not support inventory adjustments via the Accounting API
 *    (requires Xero Inventory API — returned as SKIPPED)
 */
@Component
public class XeroAdapter extends BaseOAuth2Adapter {

    /** Xero uses "/Date(ms)/" format for dates in its API */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private AccountingProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public XeroAdapter(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public AccountingProviderType getProviderType() {
        return AccountingProviderType.XERO;
    }

    // -------------------------------------------------------------------------
    // OAuth2
    // -------------------------------------------------------------------------

    @Override
    public String buildAuthorizationUrl(String state) {
        AccountingProperties.XeroConfig cfg = properties.getXero();
        return UriComponentsBuilder.fromHttpUrl(cfg.getAuthorizationUrl())
                .queryParam("client_id", cfg.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email accounting.transactions offline_access")
                .queryParam("redirect_uri", cfg.getRedirectUri())
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public AccountingCredentials exchangeCodeForTokens(String code, AccountingCredentials credentials) {
        AccountingProperties.XeroConfig cfg = properties.getXero();
        Map<String, Object> response = exchangeCode(
                cfg.getTokenUrl(), cfg.getClientId(), cfg.getClientSecret(),
                code, cfg.getRedirectUri());
        applyTokenResponse(response, credentials);
        credentials.setActive(true);
        credentials.setConnectedAt(LocalDateTime.now());

        // Xero requires fetching the tenant (organisation) ID separately
        String tenantId = fetchXeroTenantId(credentials);
        credentials.setTenantId(tenantId);
        return credentials;
    }

    @Override
    public AccountingCredentials refreshAccessToken(AccountingCredentials credentials) {
        AccountingProperties.XeroConfig cfg = properties.getXero();
        Map<String, Object> response = refreshToken(
                cfg.getTokenUrl(), cfg.getClientId(), cfg.getClientSecret(),
                credentials.getRefreshToken());
        applyTokenResponse(response, credentials);
        return credentials;
    }

    @Override
    public void disconnect(AccountingCredentials credentials) {
        try {
            AccountingProperties.XeroConfig cfg = properties.getXero();
            Map<String, String> body = Map.of("token", credentials.getRefreshToken());
            restTemplate.postForObject(cfg.getRevokeUrl(),
                    new HttpEntity<>(body, bearerHeaders(credentials)), Void.class);
        } catch (Exception ex) {
            log.warn("Xero token revoke failed (continuing): {}", ex.getMessage());
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
                    .errorMessage("Xero HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString())
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
            line.put("Description", item.getDescription());
            line.put("Quantity", item.getQuantity());
            line.put("UnitAmount", item.getUnitPrice());
            line.put("TaxType", item.getTaxRate() > 0
                    ? properties.getXero().getTaxAccountCode() : "NONE");
            line.put("AccountCode", properties.getXero().getRevenueAccountCode());
            lineItems.add(line);
        }

        Map<String, Object> invoice = new HashMap<>();
        invoice.put("Type", "ACCREC");
        invoice.put("Status", "AUTHORISED");
        invoice.put("InvoiceNumber", request.getReferenceNumber());
        invoice.put("Date", request.getDate().format(ISO_DATE));
        invoice.put("DueDate", request.getDate().format(ISO_DATE));
        invoice.put("CurrencyCode", request.getCurrency());
        invoice.put("LineItems", lineItems);
        invoice.put("Reference",
                request.getPaymentMethod() + (request.getNotes() != null ? " — " + request.getNotes() : ""));

        if (request.getContact() != null && request.getContact().getName() != null) {
            invoice.put("Contact", Map.of("Name", request.getContact().getName()));
        } else {
            invoice.put("Contact", Map.of("Name", "Walk-in Customer"));
        }

        Map<String, Object> payload = Map.of("Invoices", List.of(invoice));
        Map<String, Object> response = postToXero(
                "/Invoices", payload, credentials);

        return extractInvoiceResult(response);
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult createCreditNote(AccountingInvoiceRequest request,
            AccountingCredentials credentials) {

        List<Map<String, Object>> lineItems = new ArrayList<>();
        for (AccountingLineItem item : request.getLineItems()) {
            Map<String, Object> line = new HashMap<>();
            line.put("Description", item.getDescription());
            line.put("Quantity", item.getQuantity());
            line.put("UnitAmount", item.getUnitPrice());
            line.put("AccountCode", properties.getXero().getRevenueAccountCode());
            lineItems.add(line);
        }

        Map<String, Object> creditNote = new HashMap<>();
        creditNote.put("Type", "ACCRECCREDIT");
        creditNote.put("Status", "AUTHORISED");
        creditNote.put("CreditNoteNumber", request.getReferenceNumber());
        creditNote.put("Date", request.getDate().format(ISO_DATE));
        creditNote.put("CurrencyCode", request.getCurrency());
        creditNote.put("LineItems", lineItems);
        creditNote.put("Reference", "Return for: " + request.getOriginalExternalReference());

        if (request.getContact() != null && request.getContact().getName() != null) {
            creditNote.put("Contact", Map.of("Name", request.getContact().getName()));
        } else {
            creditNote.put("Contact", Map.of("Name", "Walk-in Customer"));
        }

        Map<String, Object> payload = Map.of("CreditNotes", List.of(creditNote));
        Map<String, Object> response = postToXero("/CreditNotes", payload, credentials);

        return extractCreditNoteResult(response);
    }

    // -------------------------------------------------------------------------
    // Stock adjustment — not supported in Xero Accounting API
    // -------------------------------------------------------------------------

    @Override
    public AccountingSyncResult syncStockAdjustment(AccountingStockAdjustmentRequest request,
            AccountingCredentials credentials) {
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SKIPPED)
                .errorMessage("Xero Accounting API does not support inventory adjustments. "
                        + "Use the Xero Inventory app for stock management.")
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String fetchXeroTenantId(AccountingCredentials credentials) {
        try {
            HttpHeaders headers = bearerHeaders(credentials);
            ResponseEntity<List> response = restTemplate.exchange(
                    properties.getXero().getConnectionsUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class);
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> connection = (Map<String, Object>) response.getBody().get(0);
                String tenantId = (String) connection.get("tenantId");
                String tenantName = (String) connection.get("tenantName");
                credentials.setOrganisationName(tenantName);
                return tenantId;
            }
        } catch (Exception ex) {
            log.warn("Could not fetch Xero tenant ID: {}", ex.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postToXero(String path, Object payload,
            AccountingCredentials credentials) {
        HttpHeaders headers = bearerHeaders(credentials);
        headers.set("Xero-Tenant-Id", credentials.getTenantId());
        String url = properties.getXero().getApiBase() + path;
        return restTemplate.postForObject(url, new HttpEntity<>(payload, headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult extractInvoiceResult(Map<String, Object> response) {
        if (response == null) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("Null Xero response").build();
        }
        List<Map<String, Object>> invoices = (List<Map<String, Object>>) response.get("Invoices");
        if (invoices == null || invoices.isEmpty()) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("No Invoices in Xero response").build();
        }
        Map<String, Object> inv = invoices.get(0);
        String id = (String) inv.get("InvoiceID");
        String number = (String) inv.get("InvoiceNumber");
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SYNCED)
                .externalReference("XERO-INV-" + number)
                .externalUrl("https://go.xero.com/AccountsReceivable/View.aspx?InvoiceID=" + id)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult extractCreditNoteResult(Map<String, Object> response) {
        if (response == null) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("Null Xero response").build();
        }
        List<Map<String, Object>> notes = (List<Map<String, Object>>) response.get("CreditNotes");
        if (notes == null || notes.isEmpty()) {
            return AccountingSyncResult.builder().status(AccountingSyncStatus.FAILED)
                    .errorMessage("No CreditNotes in Xero response").build();
        }
        Map<String, Object> note = notes.get(0);
        String id = (String) note.get("CreditNoteID");
        String number = (String) note.get("CreditNoteNumber");
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SYNCED)
                .externalReference("XERO-CN-" + number)
                .externalUrl("https://go.xero.com/AccountsReceivable/ViewCreditNote.aspx?creditNoteID=" + id)
                .build();
    }
}
