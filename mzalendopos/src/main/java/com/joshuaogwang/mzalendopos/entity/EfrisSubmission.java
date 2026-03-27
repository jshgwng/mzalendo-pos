package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

/**
 * Audit trail for every EFRIS invoice submission to URA.
 * Enables retry on failure and offline queuing.
 */
@Entity
@Data
@Table(name = "efris_submissions")
public class EfrisSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    @ToString.Exclude
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EfrisSubmissionStatus status = EfrisSubmissionStatus.PENDING;

    /** Fiscal Receipt Number returned by URA on success */
    @Column(nullable = true)
    private String fiscalReceiptNumber;

    /** Base64-encoded QR code returned by URA */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String qrCode;

    /** URA verification / anti-fake code */
    @Column(nullable = true)
    private String antifakeCode;

    /** Raw error message from URA if submission failed */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String errorMessage;

    /** URA return code (e.g. "00" = success) */
    @Column(nullable = true)
    private String uraReturnCode;

    /** Number of times submission was attempted */
    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime submittedAt;

    @Column(nullable = true)
    private LocalDateTime lastAttemptAt;
}
