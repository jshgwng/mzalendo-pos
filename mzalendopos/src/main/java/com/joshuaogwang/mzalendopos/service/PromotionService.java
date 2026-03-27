package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.PromotionRequest;
import com.joshuaogwang.mzalendopos.entity.Promotion;
import com.joshuaogwang.mzalendopos.entity.SaleItem;

public interface PromotionService {

    Promotion createPromotion(PromotionRequest request);

    Promotion updatePromotion(Long id, PromotionRequest request);

    Promotion activatePromotion(Long id);

    Promotion deactivatePromotion(Long id);

    Page<Promotion> getAllPromotions(Pageable pageable);

    List<Promotion> getActivePromotions();

    double calculatePromotionDiscount(List<SaleItem> items, double cartTotal);
}
