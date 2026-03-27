package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;

    // ── Condition fields ─────────────────────────────────────────────────────
    /** Minimum spend to trigger (SPEND_X_GET_Y_OFF) or buy quantity (BUY_X_GET_Y_FREE) */
    @Column(nullable = true)
    private Double conditionValue;

    /** Product the buy-X condition applies to (BUY_X_GET_Y_FREE) — null = any */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_product_id", nullable = true)
    @ToString.Exclude
    private Product conditionProduct;

    // ── Reward fields ────────────────────────────────────────────────────────
    /** Percentage off or fixed UGX amount off or free qty */
    @Column(nullable = false)
    private double rewardValue;

    /** Product to give free (BUY_X_GET_Y_FREE) — null = same product */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_product_id", nullable = true)
    @ToString.Exclude
    private Product rewardProduct;

    // ── Validity ─────────────────────────────────────────────────────────────
    @Column(nullable = true)
    private LocalDateTime startsAt;

    @Column(nullable = true)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private int usageCount = 0;

    /** 0 = unlimited */
    @Column(nullable = false)
    private int usageLimit = 0;

    public boolean isCurrentlyActive() {
        if (status != PromotionStatus.ACTIVE) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (endsAt != null && now.isAfter(endsAt)) return false;
        if (usageLimit > 0 && usageCount >= usageLimit) return false;
        return true;
    }
}
