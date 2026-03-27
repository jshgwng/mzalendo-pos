package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "credit_transactions")
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private CustomerAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionType type;

    @Column(nullable = false)
    private double amount;

    /** Balance after this transaction */
    @Column(nullable = false)
    private double balanceAfter;

    /** Reference to the sale for CHARGE type */
    @Column(nullable = true)
    private String saleReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by", nullable = false)
    @ToString.Exclude
    private User processedBy;

    @Column(nullable = false)
    private LocalDateTime transactionAt;

    @Column(nullable = true, length = 500)
    private String notes;
}
