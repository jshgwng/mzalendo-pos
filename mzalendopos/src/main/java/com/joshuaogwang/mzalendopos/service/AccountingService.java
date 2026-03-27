package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSync;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleReturn;
import com.joshuaogwang.mzalendopos.entity.StockAdjustment;

public interface AccountingService {

    // -------------------------------------------------------------------------
    // OAuth2 connection management
    // -------------------------------------------------------------------------

    /** Generate the OAuth2 authorization URL for a provider. */
    String getAuthorizationUrl(AccountingProviderType provider);

    /**
     * Handle the OAuth2 callback — exchange code for tokens and persist
     * the connected credentials.
     */
    AccountingCredentials handleOAuthCallback(AccountingProviderType provider, String code);

    /** Disconnect (revoke tokens and deactivate) a provider. */
    void disconnect(AccountingProviderType provider);

    /** Return all stored credentials (active and inactive). */
    List<AccountingCredentials> getAllCredentials();

    AccountingCredentials getCredentials(AccountingProviderType provider);

    // -------------------------------------------------------------------------
    // Sync triggers — called by POS services
    // -------------------------------------------------------------------------

    /**
     * Sync a completed sale to all active accounting providers.
     * Failures are logged and queued for retry; the caller is never blocked.
     *
     * @return list of sync records created (one per active provider)
     */
    List<AccountingSync> syncSale(Sale sale);

    /**
     * Sync a processed return (credit note) to all active accounting providers.
     */
    List<AccountingSync> syncReturn(SaleReturn saleReturn);

    /**
     * Sync a stock adjustment to providers that support inventory management.
     */
    List<AccountingSync> syncStockAdjustment(StockAdjustment adjustment);

    // -------------------------------------------------------------------------
    // Retry and monitoring
    // -------------------------------------------------------------------------

    /**
     * Retry all PENDING/RETRY syncs that have not exceeded the max attempt limit.
     * Called by the internal scheduler; can also be triggered manually via the API.
     */
    List<AccountingSync> retryPendingSyncs();

    Page<AccountingSync> getSyncHistory(Pageable pageable);

    List<AccountingSync> getSyncHistoryForReference(String posReference);
}
