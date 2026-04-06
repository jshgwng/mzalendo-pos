package com.joshuaogwang.mzalendopos.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.CheckoutRequest;
import com.joshuaogwang.mzalendopos.dto.DiscountRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiptResponse;
import com.joshuaogwang.mzalendopos.dto.SaleItemRequest;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.service.SaleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales", description = "Manage sales transactions from open to checkout")
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    @Autowired
    private SaleService saleService;

    @PostMapping
    @Operation(summary = "Open new sale", description = "Creates a new OPEN sale assigned to the authenticated cashier")
    @ApiResponse(responseCode = "201", description = "Sale opened")
    public ResponseEntity<Sale> openSale(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.openSale(userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List sales", description = "Returns a paginated list of sales, optionally filtered by status and date range")
    @ApiResponse(responseCode = "200", description = "Sales retrieved successfully")
    public ResponseEntity<Page<Sale>> getAllSales(
            @Parameter(description = "Filter by sale status (OPEN, COMPLETED, VOIDED, RETURNED, PARTIALLY_RETURNED)")
            @RequestParam(required = false) SaleStatus status,
            @Parameter(description = "Start datetime (ISO 8601, e.g. 2024-01-01T00:00:00)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End datetime (ISO 8601, e.g. 2024-01-31T23:59:59)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(saleService.getAllSales(status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID")
    @ApiResponse(responseCode = "200", description = "Sale found")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping("/cashier/{cashierId}")
    @Operation(summary = "Get sales by cashier")
    @ApiResponse(responseCode = "200", description = "Sales retrieved successfully")
    public ResponseEntity<Page<Sale>> getSalesByCashier(
            @PathVariable Long cashierId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(saleService.getSalesByCashier(cashierId, pageable));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add item to sale", description = "Adds a product line item to an open sale")
    @ApiResponse(responseCode = "201", description = "Item added")
    @ApiResponse(responseCode = "404", description = "Sale or product not found")
    public ResponseEntity<SaleItem> addItem(
            @PathVariable Long id,
            @Valid @RequestBody SaleItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "Update item quantity")
    @ApiResponse(responseCode = "200", description = "Quantity updated")
    @ApiResponse(responseCode = "404", description = "Sale or item not found")
    public ResponseEntity<SaleItem> updateItemQuantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Parameter(description = "New quantity (must be > 0)") @RequestParam int quantity) {
        return ResponseEntity.ok(saleService.updateItemQuantity(id, itemId, quantity));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Remove item from sale")
    @ApiResponse(responseCode = "204", description = "Item removed")
    @ApiResponse(responseCode = "404", description = "Sale or item not found")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        saleService.removeItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/discount")
    @Operation(summary = "Apply discount", description = "Applies a percentage or fixed-amount discount to the sale")
    @ApiResponse(responseCode = "200", description = "Discount applied")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<Sale> applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(saleService.applyDiscount(id, request));
    }

    @DeleteMapping("/{id}/discount")
    @Operation(summary = "Remove discount")
    @ApiResponse(responseCode = "200", description = "Discount removed")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<Sale> removeDiscount(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.removeDiscount(id));
    }

    @PostMapping("/{id}/checkout")
    @Operation(summary = "Checkout sale", description = "Completes the sale, records payment, and updates stock levels")
    @ApiResponse(responseCode = "200", description = "Sale completed")
    @ApiResponse(responseCode = "400", description = "Sale cannot be checked out (e.g. already completed or empty)")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<Sale> checkout(
            @PathVariable Long id,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(saleService.checkout(id, request));
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Void sale", description = "Cancels an open sale and restores stock levels")
    @ApiResponse(responseCode = "200", description = "Sale voided")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<Sale> voidSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.voidSale(id));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Get receipt", description = "Returns a formatted receipt for a completed sale")
    @ApiResponse(responseCode = "200", description = "Receipt retrieved")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getReceipt(id));
    }
}
