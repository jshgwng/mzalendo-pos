package com.joshuaogwang.mzalendopos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Product name is required")
    private String name;

    @Column(unique = true, nullable = true)
    private String barcode;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @Column(nullable = true)
    private String variation;

    @Min(value = 0, message = "Cost price must be non-negative")
    private double costPrice;

    @Min(value = 0, message = "Selling price must be non-negative")
    private double sellingPrice;

    @Min(value = 0, message = "Stock level must be non-negative")
    private int stockLevel;

    @Column(nullable = false)
    private double taxRate = 0.0;

    @Column(nullable = true)
    private String imageUrl;

    // Reorder management
    @Column(nullable = false)
    private int reorderPoint = 0;

    @Column(nullable = false)
    private int reorderQuantity = 0;

    @Column(nullable = false)
    private boolean lowStockAlertEnabled = false;

    /** True when this product uses ProductVariant records for stock/pricing */
    @Column(nullable = false)
    private boolean hasVariants = false;

    /** Track batches/expiry dates (perishables, pharmaceuticals, etc.) */
    @Column(nullable = false)
    private boolean trackExpiry = false;
}
