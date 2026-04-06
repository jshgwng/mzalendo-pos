package com.joshuaogwang.mzalendopos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.StockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.entity.StockAdjustment;
import com.joshuaogwang.mzalendopos.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Manage stock levels and view adjustment history")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/adjust")
    @Operation(summary = "Adjust stock", description = "Records a manual stock adjustment (restock, damage, theft, correction, etc.)")
    @ApiResponse(responseCode = "201", description = "Adjustment recorded")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<StockAdjustment> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.adjustStock(request, userDetails.getUsername()));
    }

    @GetMapping("/adjustments")
    @Operation(summary = "List stock adjustments", description = "Returns a paginated list of all stock adjustment records")
    @ApiResponse(responseCode = "200", description = "Adjustments retrieved successfully")
    public ResponseEntity<Page<StockAdjustment>> getAllAdjustments(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllAdjustments(pageable));
    }

    @GetMapping("/adjustments/product/{productId}")
    @Operation(summary = "Get adjustments by product", description = "Returns a paginated list of stock adjustments for a specific product")
    @ApiResponse(responseCode = "200", description = "Adjustments retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<Page<StockAdjustment>> getAdjustmentsByProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAdjustmentsByProduct(productId, pageable));
    }
}
