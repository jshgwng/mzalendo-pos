package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Stores OAuth2 tokens for each connected accounting provider.
 * One row per provider; updated in-place on every token refresh.
 */
@Entity
@Data
@Table(name = "accounting_credentials")
public class AccountingCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private AccountingProviderType provider;

    /** Whether this provider connection is active */
    @Column(nullable = false)
    private boolean active = false;

    /** OAuth2 access token */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String accessToken;

    /** OAuth2 refresh token (long-lived) */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String refreshToken;

    /** When the access token expires */
    @Column(nullable = true)
    private LocalDateTime tokenExpiresAt;

    /**
     * Provider-specific tenant/organisation identifier:
     * - QuickBooks: realmId (company ID)
     * - Xero: tenantId
     * - Zoho Books: organizationId
     */
    @Column(nullable = true)
    private String tenantId;

    /** Human-readable name of the connected organisation */
    @Column(nullable = true)
    private String organisationName;

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    @Column(nullable = true)
    private LocalDateTime lastRefreshedAt;

    public boolean isTokenExpired() {
        return tokenExpiresAt == null || LocalDateTime.now().isAfter(tokenExpiresAt.minusMinutes(5));
    }
}
