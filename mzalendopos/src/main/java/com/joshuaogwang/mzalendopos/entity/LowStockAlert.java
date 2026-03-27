package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "low_stock_alerts")
public class LowStockAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(nullable = false)
    private int stockLevelAtAlert;

    @Column(nullable = false)
    private int reorderPoint;

    @Column(nullable = false)
    private LocalDateTime alertedAt;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = true)
    private LocalDateTime resolvedAt;
}
