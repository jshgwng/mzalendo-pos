package com.joshuaogwang.mzalendopos.service.accounting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
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

/**
 * QuickBooks Online (QBO) adapter.
 *
 * API reference: https://developer.intuit.com/app/developer/qbo/docs/api/accounting/all-entities
 *
 * Implements:
 *  - SalesReceipt for completed cash/card/mobile sales (immediate payment)
 *  - CreditMemo for returns
 *  - InventoryAdjustment for stock adjustments (requires QBO Plus/Advanced)
 */
@Component
public class QuickBooksAdapter extends BaseOAuth2Adapter {

    private static final DateTimeFormatter QBO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AccountingProperties properties;

    @Autowired
    public QuickBooksAdapter(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public AccountingProviderType getProviderType() {
        return AccountingProviderType.QUICKBOOKS;
    }

    // -------------------------------------------------------------------------
    // OAuth2
    // -------------------------------------------------------------------------

    @Override
    public String buildAuthorizationUrl(String state) {
        AccountingProperties.QuickBooksConfig cfg = properties.getQuickbooks();
        return UriComponentsBuilder.fromHttpUrl(cfg.getAuthorizationUrl())
                .queryParam("client_id", cfg.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", "com.intuit.quickbooks.accounting")
                .queryParam("redirect_uri", cfg.getRedirectUri())
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public AccountingCredentials exchangeCodeForTokens(String code, AccountingCredentials credentials) {
        AccountingProperties.QuickBooksConfig cfg = properties.getQuickbooks();
        Map<String, Object> response = exchangeCode(
                cfg.getTokenUrl(), cfg.getClientId(), cfg.getClientSecret(),
                code, cfg.getRedirectUri());
        applyTokenResponse(response, credentials);
        credentials.setActive(true);
        credentials.setConnectedAt(LocalDateTime.now());
        return credentials;
    }

    @Override
    public AccountingCredentials refreshAccessToken(AccountingCredentials credentials) {
        AccountingProperties.QuickBooksConfig cfg = properties.getQuickbooks();
        Map<String, Object> response = refreshToken(
                cfg.getTokenUrl(), cfg.getClientId(), cfg.getClientSecret(),
                credentials.getRefreshToken());
        applyTokenResponse(response, credentials);
        return credentials;
    }

    @Override
    public void disconnect(AccountingCredentials credentials) {
        try {
            AccountingProperties.QuickBooksConfig cfg = properties.getQuickbooks();
            Map<String, String> body = new HashMap<>();
            body.put("token", credentials.getRefreshToken());
            restTemplate.postForObject(cfg.getRevokeUrl(),
                    new HttpEntity<>(body, bearerHeaders(credentials)), Void.class);
        } catch (Exception ex) {
            log.warn("QuickBooks revoke token failed (continuing disconnect): {}", ex.getMessage());
        }
        credentials.setActive(false);
        credentials.setAccessToken(null);
        credentials.setRefreshToken(null);
    }

    // -------------------------------------------------------------------------
    // Invoice sync
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public AccountingSyncResult syncInvoice(AccountingInvoiceRequest request,
            AccountingCredentials credentials) {
        try {
            String apiBase = properties.getQuickbooks().getApiBase();
            String realmId = credentials.getTenantId();

            if (request.getEventType() == AccountingEventType.RETURN_PROCESSED) {
                return createCreditMemo(request, credentials, apiBase, realmId);
            }
            return createSalesReceipt(request, credentials, apiBase, realmId);

        } catch (HttpClientErrorException ex) {
            String msg = "QBO HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString();
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage(msg)
                    .build();
        } catch (Exception ex) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.RETRY)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult createSalesReceipt(AccountingInvoiceRequest request,
            AccountingCredentials credentials, String apiBase, String realmId) {

        List<Map<String, Object>> lines = new ArrayList<>();
        int lineNum = 1;
        for (AccountingLineItem item : request.getLineItems()) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("Qty", item.getQuantity());
            detail.put("UnitPrice", item.getUnitPrice());
            detail.put("ServiceDate", request.getDate().format(QBO_DATE));

            Map<String, Object> line = new HashMap<>();
            line.put("LineNum", lineNum++);
            line.put("Description", item.getDescription());
            line.put("Amount", item.getLineTotal());
            line.put("DetailType", "SalesItemLineDetail");
            line.put("SalesItemLineDetail", detail);
            lines.add(line);
        }

        if (request.getDiscountAmount() > 0) {
            Map<String, Object> discountDetail = new HashMap<>();
            discountDetail.put("PercentBased", false);
            discountDetail.put("DiscountAccountRef", Map.of("name", "Discounts"));

            Map<String, Object> discountLine = new HashMap<>();
            discountLine.put("Description", "Discount");
            discountLine.put("Amount", request.getDiscountAmount());
            discountLine.put("DetailType", "DiscountLineDetail");
            discountLine.put("DiscountLineDetail", discountDetail);
            lines.add(discountLine);
        }

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("DocNumber", request.getReferenceNumber());
        receipt.put("TxnDate", request.getDate().format(QBO_DATE));
        receipt.put("Line", lines);
        receipt.put("PaymentMethodRef", Map.of("name", request.getPaymentMethod()));
        receipt.put("PrivateNote", request.getNotes() != null ? request.getNotes() : "");

        if (request.getContact() != null && request.getContact().getName() != null) {
            receipt.put("CustomerRef", Map.of("name", request.getContact().getName()));
        }

        String url = apiBase + "/" + realmId + "/salesreceipt?minorversion=65";
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(receipt, bearerHeaders(credentials)), Map.class);

        return extractSalesReceiptResult(response, apiBase, realmId);
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult createCreditMemo(AccountingInvoiceRequest request,
            AccountingCredentials credentials, String apiBase, String realmId) {

        List<Map<String, Object>> lines = new ArrayList<>();
        for (AccountingLineItem item : request.getLineItems()) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("Qty", item.getQuantity());
            detail.put("UnitPrice", item.getUnitPrice());

            Map<String, Object> line = new HashMap<>();
            line.put("Description", item.getDescription());
            line.put("Amount", item.getLineTotal());
            line.put("DetailType", "SalesItemLineDetail");
            line.put("SalesItemLineDetail", detail);
            lines.add(line);
        }

        Map<String, Object> creditMemo = new HashMap<>();
        creditMemo.put("DocNumber", request.getReferenceNumber());
        creditMemo.put("TxnDate", request.getDate().format(QBO_DATE));
        creditMemo.put("Line", lines);
        creditMemo.put("PrivateNote",
                "Return for: " + request.getOriginalExternalReference());

        if (request.getContact() != null && request.getContact().getName() != null) {
            creditMemo.put("CustomerRef", Map.of("name", request.getContact().getName()));
        }

        String url = apiBase + "/" + realmId + "/creditmemo?minorversion=65";
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(creditMemo, bearerHeaders(credentials)), Map.class);

        Map<String, Object> body = (Map<String, Object>) response.get("CreditMemo");
        if (body == null) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage("Empty CreditMemo in QBO response")
                    .build();
        }
        String id = String.valueOf(body.get("Id"));
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SYNCED)
                .externalReference("QBOCM-" + id)
                .externalUrl(buildQboUrl(properties.getQuickbooks().isSandbox(), realmId,
                        "creditmemo", id))
                .build();
    }

    @SuppressWarnings("unchecked")
    private AccountingSyncResult extractSalesReceiptResult(Map<String, Object> response,
            String apiBase, String realmId) {
        if (response == null) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage("Null response from QBO")
                    .build();
        }
        Map<String, Object> receipt = (Map<String, Object>) response.get("SalesReceipt");
        if (receipt == null) {
            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.FAILED)
                    .errorMessage("Missing SalesReceipt in QBO response")
                    .build();
        }
        String id = String.valueOf(receipt.get("Id"));
        return AccountingSyncResult.builder()
                .status(AccountingSyncStatus.SYNCED)
                .externalReference("QBOSR-" + id)
                .externalUrl(buildQboUrl(properties.getQuickbooks().isSandbox(), realmId,
                        "salesreceipt", id))
                .build();
    }

    // -------------------------------------------------------------------------
    // Stock adjustment
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public AccountingSyncResult syncStockAdjustment(AccountingStockAdjustmentRequest request,
            AccountingCredentials credentials) {
        try {
            // QBO inventory adjustment via InventoryAdjustment entity
            // Requires QBO Plus or Advanced plan
            String realmId = credentials.getTenantId();
            String url = properties.getQuickbooks().getApiBase()
                    + "/" + realmId + "/inventoryadjustment?minorversion=65";

            Map<String, Object> itemLine = new HashMap<>();
            itemLine.put("Name", request.getItemName());
            itemLine.put("QuantityDiff", request.getQuantityChange());
            if (request.getUnitCost() > 0) {
                itemLine.put("CurrentValue", request.getUnitCost() * Math.abs(request.getQuantityChange()));
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("TxnDate", request.getDate().format(QBO_DATE));
            payload.put("PrivateNote", request.getReason() != null ? request.getReason() : "");
            payload.put("Line", List.of(Map.of(
                    "ItemRef", Map.of("name", request.getItemName()),
                    "QuantityDiff", request.getQuantityChange())));

            Map<String, Object> response = restTemplate.postForObject(
                    url, new HttpEntity<>(payload, bearerHeaders(credentials)), Map.class);

            return AccountingSyncResult.builder()
                    .status(AccountingSyncStatus.SYNCED)
                    .externalReference("QBOIA-" + extractId(response, "InventoryAdjustment"))
                    .build();

        } catch (Exception ex) {
            log.warn("QBO stock adjustment sync failed: {}", ex.getMessage());
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
    private String extractId(Map<String, Object> response, String entityKey) {
        if (response == null) return "?";
        Map<String, Object> entity = (Map<String, Object>) response.get(entityKey);
        return entity != null ? String.valueOf(entity.get("Id")) : "?";
    }

    private String buildQboUrl(boolean sandbox, String realmId, String entity, String id) {
        String base = sandbox
                ? "https://app.sandbox.qbo.intuit.com/app/"
                : "https://app.qbo.intuit.com/app/";
        return base + entity + "?txnId=" + id;
    }
}
