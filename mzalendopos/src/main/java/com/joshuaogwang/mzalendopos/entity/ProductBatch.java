package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "product_batches")
public class ProductBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    @ToString.Exclude
    private ProductVariant variant;

    @Column(nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private int quantity;

    /** Remaining quantity in this batch */
    @Column(nullable = false)
    private int remainingQuantity;

    @Column(nullable = false)
    private double unitCost;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /** Link back to the PO that brought this batch in */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = true)
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;
}
