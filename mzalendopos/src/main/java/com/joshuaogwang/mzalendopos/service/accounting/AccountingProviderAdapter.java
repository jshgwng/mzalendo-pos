package com.joshuaogwang.mzalendopos.service.accounting;

import com.joshuaogwang.mzalendopos.dto.accounting.AccountingInvoiceRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingStockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.dto.accounting.AccountingSyncResult;
import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;

/**
 * Strategy interface implemented by each accounting provider adapter.
 * Implementations are responsible for:
 * <ul>
 *   <li>Translating the normalized request DTO to their API format</li>
 *   <li>Making the authenticated HTTP call</li>
 *   <li>Returning a {@link AccountingSyncResult} with the external reference</li>
 * </ul>
 */
public interface AccountingProviderAdapter {

    AccountingProviderType getProviderType();

    /**
     * Build the OAuth2 authorisation URL for the initial connection flow.
     *
     * @param state random CSRF-protection value
     * @return URL the user's browser should be redirected to
     */
    String buildAuthorizationUrl(String state);

    /**
     * Exchange an authorisation code for access + refresh tokens and persist
     * them in the supplied credentials record.
     */
    AccountingCredentials exchangeCodeForTokens(String code, AccountingCredentials credentials);

    /**
     * Refresh the access token using the stored refresh token.
     * Updates {@code credentials} in place.
     */
    AccountingCredentials refreshAccessToken(AccountingCredentials credentials);

    /**
     * Disconnect the provider by revoking tokens and marking the credentials
     * record inactive.
     */
    void disconnect(AccountingCredentials credentials);

    /**
     * Create or update an invoice / sales receipt / credit note in the
     * accounting tool from the normalized request.
     */
    AccountingSyncResult syncInvoice(AccountingInvoiceRequest request,
                                     AccountingCredentials credentials);

    /**
     * Record an inventory/stock adjustment. Providers that do not support
     * inventory management should return a SKIPPED result.
     */
    AccountingSyncResult syncStockAdjustment(AccountingStockAdjustmentRequest request,
                                              AccountingCredentials credentials);
}
