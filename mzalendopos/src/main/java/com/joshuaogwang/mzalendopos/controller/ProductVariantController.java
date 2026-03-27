package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.ProductVariantRequest;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;
import com.joshuaogwang.mzalendopos.service.ProductVariantService;

@RestController
public class ProductVariantController {

    @Autowired
    private ProductVariantService variantService;

    /**
     * POST /api/v1/products/{productId}/variants
     * Add a new variant to the specified product.
     */
    @PostMapping("/api/v1/products/{productId}/variants")
    public ResponseEntity<ProductVariant> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        request.setProductId(productId);
        ProductVariant variant = variantService.addVariant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    }

    /**
     * PUT /api/v1/products/{productId}/variants/{variantId}
     * Update an existing variant.
     */
    @PutMapping("/api/v1/products/{productId}/variants/{variantId}")
    public ResponseEntity<ProductVariant> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        request.setProductId(productId);
        ProductVariant variant = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(variant);
    }

    /**
     * GET /api/v1/products/{productId}/variants
     * Get all variants for the specified product.
     */
    @GetMapping("/api/v1/products/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getVariantsByProduct(@PathVariable Long productId) {
        List<ProductVariant> variants = variantService.getVariantsByProduct(productId);
        return ResponseEntity.ok(variants);
    }

    /**
     * DELETE /api/v1/products/{productId}/variants/{variantId}
     * Deactivate a variant (soft delete).
     */
    @DeleteMapping("/api/v1/products/{productId}/variants/{variantId}")
    public ResponseEntity<Void> deactivateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        variantService.deactivateVariant(variantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/variants/barcode/{barcode}
     * Look up a variant by barcode.
     */
    @GetMapping("/api/v1/variants/barcode/{barcode}")
    public ResponseEntity<ProductVariant> getByBarcode(@PathVariable String barcode) {
        ProductVariant variant = variantService.getVariantByBarcode(barcode);
        return ResponseEntity.ok(variant);
    }
}
