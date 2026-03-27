package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSync;
import com.joshuaogwang.mzalendopos.service.AccountingService;

/**
 * REST endpoints for managing accounting tool integrations.
 *
 * Connection flow:
 *   1. GET  /api/v1/accounting/{provider}/connect   → redirects user to OAuth2 login
 *   2. GET  /api/v1/accounting/{provider}/callback  → receives OAuth2 code, exchanges for tokens
 *   3. DELETE /api/v1/accounting/{provider}/connect → disconnects the provider
 *
 * Monitoring:
 *   GET  /api/v1/accounting/connections              → list all provider connections
 *   GET  /api/v1/accounting/syncs                    → paginated sync history
 *   GET  /api/v1/accounting/syncs/sale/{reference}   → syncs for a specific POS reference
 *   POST /api/v1/accounting/retry                    → manually trigger retry of pending syncs
 */
@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {

    @Autowired
    private AccountingService accountingService;

    // -------------------------------------------------------------------------
    // OAuth2 connection management
    // -------------------------------------------------------------------------

    /**
     * Return the OAuth2 authorization URL for a provider.
     * The frontend should redirect the user's browser to this URL.
     */
    @GetMapping("/{provider}/connect")
    public ResponseEntity<String> getConnectUrl(
            @PathVariable AccountingProviderType provider) {
        return ResponseEntity.ok(accountingService.getAuthorizationUrl(provider));
    }

    /**
     * OAuth2 callback endpoint. The accounting provider redirects here after
     * the user grants access. Exchanges the code for tokens and stores them.
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<AccountingCredentials> oauthCallback(
            @PathVariable AccountingProviderType provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        return ResponseEntity.ok(accountingService.handleOAuthCallback(provider, code));
    }

    /**
     * Revoke tokens and disconnect a provider.
     */
    @DeleteMapping("/{provider}/connect")
    public ResponseEntity<Void> disconnect(@PathVariable AccountingProviderType provider) {
        accountingService.disconnect(provider);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Connection status
    // -------------------------------------------------------------------------

    /** List all stored provider credentials (active and inactive). */
    @GetMapping("/connections")
    public ResponseEntity<List<AccountingCredentials>> getConnections() {
        return ResponseEntity.ok(accountingService.getAllCredentials());
    }

    /** Get credentials for a specific provider. */
    @GetMapping("/connections/{provider}")
    public ResponseEntity<AccountingCredentials> getConnection(
            @PathVariable AccountingProviderType provider) {
        return ResponseEntity.ok(accountingService.getCredentials(provider));
    }

    // -------------------------------------------------------------------------
    // Sync history and monitoring
    // -------------------------------------------------------------------------

    /** Paginated full sync history across all providers. */
    @GetMapping("/syncs")
    public ResponseEntity<Page<AccountingSync>> getSyncHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(accountingService.getSyncHistory(pageable));
    }

    /** All sync records for a specific POS reference (sale number, return ID, etc.) */
    @GetMapping("/syncs/reference/{posReference}")
    public ResponseEntity<List<AccountingSync>> getSyncsForReference(
            @PathVariable String posReference) {
        return ResponseEntity.ok(accountingService.getSyncHistoryForReference(posReference));
    }

    // -------------------------------------------------------------------------
    // Manual retry
    // -------------------------------------------------------------------------

    /**
     * Manually trigger a retry of all PENDING/RETRY syncs.
     * Useful when connectivity to an accounting provider is restored after an outage.
     */
    @PostMapping("/retry")
    public ResponseEntity<List<AccountingSync>> retryPendingSyncs() {
        return ResponseEntity.ok(accountingService.retryPendingSyncs());
    }
}
