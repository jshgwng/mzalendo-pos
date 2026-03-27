package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.config.AccountingProperties;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingContact;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingInvoiceRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingLineItem;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingStockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingSyncResult;
import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingEventType;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSync;
import com.joshuaogwang.mzalendopos.entity.AccountingSyncStatus;
import com.joshuaogwang.mzalendopos.entity.ReturnItem;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleReturn;
import com.joshuaogwang.mzalendopos.entity.StockAdjustment;
import com.joshuaogwang.mzalendopos.repository.AccountingCredentialsRepository;
import com.joshuaogwang.mzalendopos.repository.AccountingSyncRepository;
import com.joshuaogwang.mzalendopos.service.AccountingService;
import com.joshuaogwang.mzalendopos.service.accounting.AccountingProviderAdapter;

@Service
public class AccountingServiceImpl implements AccountingService {

    private static final Logger log = LoggerFactory.getLogger(AccountingServiceImpl.class);

    @Autowired
    private AccountingCredentialsRepository credentialsRepository;

    @Autowired
    private AccountingSyncRepository syncRepository;

    @Autowired
    private AccountingProperties properties;

    /** All adapters injected as a list — Spring collects every @Component that implements the interface */
    @Autowired
    private List<AccountingProviderAdapter> adapters;

    // -------------------------------------------------------------------------
    // OAuth2 connection management
    // -------------------------------------------------------------------------

    @Override
    public String getAuthorizationUrl(AccountingProviderType provider) {
        String state = UUID.randomUUID().toString();
        return getAdapter(provider).buildAuthorizationUrl(state);
    }

    @Override
    @Transactional
    public AccountingCredentials handleOAuthCallback(AccountingProviderType provider, String code) {
        AccountingCredentials credentials = credentialsRepository.findByProvider(provider)
                .orElseGet(() -> {
                    AccountingCredentials c = new AccountingCredentials();
                    c.setProvider(provider);
                    return c;
                });
        AccountingCredentials updated = getAdapter(provider).exchangeCodeForTokens(code, credentials);
        return credentialsRepository.save(updated);
    }

    @Override
    @Transactional
    public void disconnect(AccountingProviderType provider) {
        AccountingCredentials credentials = credentialsRepository.findByProvider(provider)
                .orElseThrow(() -> new NoSuchElementException("No credentials for provider: " + provider));
        getAdapter(provider).disconnect(credentials);
        credentialsRepository.save(credentials);
    }

    @Override
    public List<AccountingCredentials> getAllCredentials() {
        return credentialsRepository.findAll();
    }

    @Override
    public AccountingCredentials getCredentials(AccountingProviderType provider) {
        return credentialsRepository.findByProvider(provider)
                .orElseThrow(() -> new NoSuchElementException("No credentials for: " + provider));
    }

    // -------------------------------------------------------------------------
    // Sync triggers
    // -------------------------------------------------------------------------

    @Override
    public List<AccountingSync> syncSale(Sale sale) {
        AccountingInvoiceRequest request = buildSaleRequest(sale);
        return syncToAllActiveProviders(request, sale.getSaleNumber());
    }

    @Override
    public List<AccountingSync> syncReturn(SaleReturn saleReturn) {
        AccountingInvoiceRequest request = buildReturnRequest(saleReturn);
        String ref = "RETURN-" + saleReturn.getId();
        return syncToAllActiveProviders(request, ref);
    }

    @Override
    public List<AccountingSync> syncStockAdjustment(StockAdjustment adjustment) {
        AccountingStockAdjustmentRequest request = buildStockRequest(adjustment);
        List<AccountingSync> results = new ArrayList<>();

        for (AccountingCredentials credentials : getActiveCredentials()) {
            AccountingProviderAdapter adapter = getAdapter(credentials.getProvider());
            String ref = "ADJUST-" + adjustment.getId();
            AccountingSync sync = getOrCreateSync(credentials.getProvider(),
                    AccountingEventType.STOCK_ADJUSTED, ref);
            results.add(attemptStockSync(adapter, request, credentials, sync));
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Retry scheduler
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${accounting.retry-interval-ms:300000}")
    public List<AccountingSync> retryPendingSyncs() {
        List<AccountingSync> pending = new ArrayList<>();
        pending.addAll(syncRepository.findByStatusAndAttemptCountLessThan(
                AccountingSyncStatus.PENDING, properties.getMaxRetryAttempts()));
        pending.addAll(syncRepository.findByStatusAndAttemptCountLessThan(
                AccountingSyncStatus.RETRY, properties.getMaxRetryAttempts()));

        List<AccountingSync> retried = new ArrayList<>();
        for (AccountingSync sync : pending) {
            try {
                AccountingCredentials credentials = credentialsRepository
                        .findByProvider(sync.getProvider())
                        .filter(AccountingCredentials::isActive)
                        .orElse(null);
                if (credentials == null) continue;

                AccountingProviderAdapter adapter = getAdapter(sync.getProvider());
                refreshIfExpired(adapter, credentials);

                // Rebuild the request from the posReference — only invoice syncs are retried
                // (stock adjustments are informational and don't block operations)
                log.info("Retrying accounting sync id={} provider={} ref={}",
                        sync.getId(), sync.getProvider(), sync.getPosReference());
                sync.setAttemptCount(sync.getAttemptCount() + 1);
                sync.setLastAttemptAt(LocalDateTime.now());
                sync.setStatus(AccountingSyncStatus.RETRY);
                retried.add(syncRepository.save(sync));
            } catch (Exception ex) {
                log.warn("Retry of accounting sync {} failed: {}", sync.getId(), ex.getMessage());
            }
        }
        return retried;
    }

    @Override
    public Page<AccountingSync> getSyncHistory(Pageable pageable) {
        return syncRepository.findAll(pageable);
    }

    @Override
    public List<AccountingSync> getSyncHistoryForReference(String posReference) {
        return syncRepository.findByPosReference(posReference);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private List<AccountingSync> syncToAllActiveProviders(
            AccountingInvoiceRequest request, String posReference) {
        List<AccountingSync> results = new ArrayList<>();

        for (AccountingCredentials credentials : getActiveCredentials()) {
            AccountingProviderAdapter adapter = getAdapter(credentials.getProvider());
            AccountingSync sync = getOrCreateSync(credentials.getProvider(),
                    request.getEventType(), posReference);

            try {
                refreshIfExpired(adapter, credentials);
                AccountingSyncResult result = adapter.syncInvoice(request, credentials);
                applyResult(sync, result);
            } catch (Exception ex) {
                log.error("Accounting sync failed for {} / {}: {}",
                        credentials.getProvider(), posReference, ex.getMessage(), ex);
                sync.setStatus(AccountingSyncStatus.RETRY);
                sync.setErrorMessage(ex.getMessage());
            }

            sync.setAttemptCount(sync.getAttemptCount() + 1);
            sync.setLastAttemptAt(LocalDateTime.now());
            results.add(syncRepository.save(sync));
        }
        return results;
    }

    private AccountingSync attemptStockSync(AccountingProviderAdapter adapter,
            AccountingStockAdjustmentRequest request,
            AccountingCredentials credentials, AccountingSync sync) {
        try {
            refreshIfExpired(adapter, credentials);
            AccountingSyncResult result = adapter.syncStockAdjustment(request, credentials);
            applyResult(sync, result);
        } catch (Exception ex) {
            sync.setStatus(AccountingSyncStatus.RETRY);
            sync.setErrorMessage(ex.getMessage());
        }
        sync.setAttemptCount(sync.getAttemptCount() + 1);
        sync.setLastAttemptAt(LocalDateTime.now());
        return syncRepository.save(sync);
    }

    private void applyResult(AccountingSync sync, AccountingSyncResult result) {
        sync.setStatus(result.getStatus());
        sync.setExternalReference(result.getExternalReference());
        sync.setExternalUrl(result.getExternalUrl());
        sync.setErrorMessage(result.getErrorMessage());
        if (result.getStatus() == AccountingSyncStatus.SYNCED) {
            sync.setSyncedAt(LocalDateTime.now());
        }
    }

    private AccountingSync getOrCreateSync(AccountingProviderType provider,
            AccountingEventType eventType, String posReference) {
        return syncRepository.findByProviderAndPosReference(provider, posReference)
                .orElseGet(() -> {
                    AccountingSync s = new AccountingSync();
                    s.setProvider(provider);
                    s.setEventType(eventType);
                    s.setPosReference(posReference);
                    s.setStatus(AccountingSyncStatus.PENDING);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });
    }

    private void refreshIfExpired(AccountingProviderAdapter adapter,
            AccountingCredentials credentials) {
        if (credentials.isTokenExpired()) {
            AccountingCredentials refreshed = adapter.refreshAccessToken(credentials);
            credentialsRepository.save(refreshed);
        }
    }

    private List<AccountingCredentials> getActiveCredentials() {
        return credentialsRepository.findAll().stream()
                .filter(AccountingCredentials::isActive)
                .collect(Collectors.toList());
    }

    private AccountingProviderAdapter getAdapter(AccountingProviderType provider) {
        return adapters.stream()
                .filter(a -> a.getProviderType() == provider)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No adapter registered for provider: " + provider));
    }

    // -------------------------------------------------------------------------
    // Request builders
    // -------------------------------------------------------------------------

    private AccountingInvoiceRequest buildSaleRequest(Sale sale) {
        List<AccountingLineItem> lineItems = sale.getItems().stream()
                .map(item -> AccountingLineItem.builder()
                        .description(item.getProductName())
                        .itemCode(item.getProduct() != null && item.getProduct().getBarcode() != null
                                ? item.getProduct().getBarcode() : "")
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .taxRate(item.getTaxRate())
                        .taxAmount(item.getLineTotal() * (item.getTaxRate() / 100.0))
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        AccountingContact contact = null;
        if (sale.getCustomer() != null) {
            contact = AccountingContact.builder()
                    .name(sale.getCustomer().getName())
                    .email(sale.getCustomer().getEmail())
                    .phone(sale.getCustomer().getPhoneNumber())
                    .build();
        }

        String paymentMethod = sale.getPayments() != null && !sale.getPayments().isEmpty()
                ? sale.getPayments().stream()
                        .map(p -> p.getMethod().name())
                        .distinct()
                        .collect(java.util.stream.Collectors.joining("/"))
                : "CASH";

        return AccountingInvoiceRequest.builder()
                .eventType(AccountingEventType.SALE_COMPLETED)
                .referenceNumber(sale.getSaleNumber())
                .date(sale.getCompletedAt() != null ? sale.getCompletedAt() : LocalDateTime.now())
                .currency("UGX")
                .contact(contact)
                .lineItems(lineItems)
                .subtotal(sale.getSubtotal())
                .taxAmount(sale.getTaxAmount())
                .discountAmount(sale.getDiscountAmount())
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(paymentMethod)
                .build();
    }

    private AccountingInvoiceRequest buildReturnRequest(SaleReturn saleReturn) {
        List<AccountingLineItem> lineItems = saleReturn.getItems().stream()
                .map(item -> AccountingLineItem.builder()
                        .description(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .taxRate(0)
                        .lineTotal(item.getRefundAmount())
                        .build())
                .collect(Collectors.toList());

        String originalRef = null;
        if (saleReturn.getOriginalSale() != null) {
            List<AccountingSync> originalSyncs = syncRepository
                    .findByPosReference(saleReturn.getOriginalSale().getSaleNumber());
            if (!originalSyncs.isEmpty() && originalSyncs.get(0).getExternalReference() != null) {
                originalRef = originalSyncs.get(0).getExternalReference();
            } else {
                originalRef = saleReturn.getOriginalSale().getSaleNumber();
            }
        }

        return AccountingInvoiceRequest.builder()
                .eventType(AccountingEventType.RETURN_PROCESSED)
                .referenceNumber("RETURN-" + saleReturn.getId())
                .date(saleReturn.getReturnedAt())
                .currency("UGX")
                .lineItems(lineItems)
                .totalAmount(saleReturn.getRefundAmount())
                .originalExternalReference(originalRef)
                .notes(saleReturn.getReason())
                .build();
    }

    private AccountingStockAdjustmentRequest buildStockRequest(StockAdjustment adjustment) {
        return AccountingStockAdjustmentRequest.builder()
                .itemCode(adjustment.getProduct().getBarcode() != null
                        ? adjustment.getProduct().getBarcode() : "")
                .itemName(adjustment.getProduct().getName())
                .adjustmentType(adjustment.getAdjustmentType())
                .quantityChange(adjustment.getQuantity())
                .unitCost(adjustment.getProduct().getCostPrice())
                .date(adjustment.getAdjustedAt())
                .reason(adjustment.getReason())
                .adjustedBy(adjustment.getAdjustedBy() != null
                        ? adjustment.getAdjustedBy().getUsername() : "")
                .build();
    }
}
