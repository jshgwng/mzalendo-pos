package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "customer_accounts")
public class CustomerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true, nullable = false)
    @ToString.Exclude
    private Customer customer;

    @Column(nullable = false)
    private double creditLimit = 0.0;

    /** Total unpaid balance (positive = customer owes money) */
    @Column(nullable = false)
    private double outstandingBalance = 0.0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime lastTransactionAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<CreditTransaction> transactions = new ArrayList<>();
}
