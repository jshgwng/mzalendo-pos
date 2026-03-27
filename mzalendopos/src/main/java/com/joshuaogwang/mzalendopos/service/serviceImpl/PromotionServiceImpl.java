package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.PromotionRequest;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.Promotion;
import com.joshuaogwang.mzalendopos.entity.PromotionStatus;
import com.joshuaogwang.mzalendopos.entity.PromotionType;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.PromotionRepository;
import com.joshuaogwang.mzalendopos.service.PromotionService;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public Promotion createPromotion(PromotionRequest request) {
        Promotion promotion = new Promotion();
        applyRequest(promotion, request);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Promotion not found with id: " + id));
        applyRequest(promotion, request);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion activatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Promotion not found with id: " + id));
        promotion.setStatus(PromotionStatus.ACTIVE);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion deactivatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Promotion not found with id: " + id));
        promotion.setStatus(PromotionStatus.INACTIVE);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    @Override
    public double calculatePromotionDiscount(List<SaleItem> items, double cartTotal) {
        List<Promotion> activePromotions = promotionRepository.findActivePromotions(LocalDateTime.now());

        double totalDiscount = 0.0;

        for (Promotion promotion : activePromotions) {
            double discount = 0.0;

            if (promotion.getType() == PromotionType.PERCENTAGE_OFF) {
                discount = cartTotal * (promotion.getRewardValue() / 100.0);

            } else if (promotion.getType() == PromotionType.FIXED_AMOUNT_OFF) {
                discount = Math.min(promotion.getRewardValue(), cartTotal);

            } else if (promotion.getType() == PromotionType.SPEND_X_GET_Y_OFF) {
                if (promotion.getConditionValue() != null && cartTotal >= promotion.getConditionValue()) {
                    discount = promotion.getRewardValue();
                }

            } else if (promotion.getType() == PromotionType.BUY_X_GET_Y_FREE) {
                if (promotion.getConditionValue() != null) {
                    for (SaleItem item : items) {
                        boolean conditionMatches = promotion.getConditionProduct() == null ||
                                (item.getProduct() != null &&
                                 item.getProduct().getId().equals(promotion.getConditionProduct().getId()));

                        if (conditionMatches && item.getQuantity() >= promotion.getConditionValue()) {
                            double freeSets = Math.floor(item.getQuantity() / promotion.getConditionValue());
                            discount += freeSets * promotion.getRewardValue() * item.getUnitPrice();
                        }
                    }
                }
            }

            totalDiscount += discount;
        }

        // Cap discount at cart total
        return Math.min(totalDiscount, cartTotal);
    }

    private void applyRequest(Promotion promotion, PromotionRequest request) {
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setType(request.getType());
        promotion.setRewardValue(request.getRewardValue());
        promotion.setConditionValue(request.getConditionValue());
        promotion.setStartsAt(request.getStartsAt());
        promotion.setEndsAt(request.getEndsAt());
        promotion.setUsageLimit(request.getUsageLimit());

        if (request.getConditionProductId() != null) {
            Product conditionProduct = productRepository.findById(request.getConditionProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getConditionProductId()));
            promotion.setConditionProduct(conditionProduct);
        } else {
            promotion.setConditionProduct(null);
        }

        if (request.getRewardProductId() != null) {
            Product rewardProduct = productRepository.findById(request.getRewardProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getRewardProductId()));
            promotion.setRewardProduct(rewardProduct);
        } else {
            promotion.setRewardProduct(null);
        }
    }
}
