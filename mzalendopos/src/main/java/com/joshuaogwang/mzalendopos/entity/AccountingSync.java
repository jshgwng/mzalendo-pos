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
 * Audit trail for every accounting sync attempt.
 * One row per (provider × event); e.g. a sale synced to both
 * QuickBooks and Xero produces two rows.
 */
@Entity
@Data
@Table(name = "accounting_syncs")
public class AccountingSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountingProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountingEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountingSyncStatus status;

    /** POS reference: sale number or return reference */
    @Column(nullable = false)
    private String posReference;

    /**
     * ID/number assigned by the accounting tool on successful sync
     * (e.g. QuickBooks invoice ID "12", Xero invoice UUID).
     */
    @Column(nullable = true)
    private String externalReference;

    /** Deep-link URL to the record in the accounting tool's UI */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String externalUrl;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime syncedAt;

    @Column(nullable = true)
    private LocalDateTime lastAttemptAt;
}
