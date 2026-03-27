package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.PromotionRequest;
import com.joshuaogwang.mzalendopos.entity.Promotion;
import com.joshuaogwang.mzalendopos.service.PromotionService;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping
    public ResponseEntity<Promotion> createPromotion(@Valid @RequestBody PromotionRequest request) {
        Promotion promotion = promotionService.createPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(promotion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promotion> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request) {
        Promotion promotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(promotion);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Promotion> activatePromotion(@PathVariable Long id) {
        Promotion promotion = promotionService.activatePromotion(id);
        return ResponseEntity.ok(promotion);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Promotion> deactivatePromotion(@PathVariable Long id) {
        Promotion promotion = promotionService.deactivatePromotion(id);
        return ResponseEntity.ok(promotion);
    }

    @GetMapping
    public ResponseEntity<Page<Promotion>> getAllPromotions(Pageable pageable) {
        Page<Promotion> promotions = promotionService.getAllPromotions(pageable);
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return ResponseEntity.ok(promotions);
    }
}
