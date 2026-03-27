package com.joshuaogwang.mzalendopos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    /** e.g. "500ml", "Red / Large", "2kg" */
    @Column(nullable = false)
    private String variantName;

    @Column(unique = true, nullable = true)
    private String sku;

    @Column(unique = true, nullable = true)
    private String barcode;

    @Min(0)
    private double sellingPrice;

    @Min(0)
    private double costPrice;

    @Min(0)
    private int stockLevel;

    @Column(nullable = false)
    private boolean active = true;
}
