package com.joshuaogwang.mzalendopos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.joshuaogwang.mzalendopos.dto.ReceiptResponse;
import com.joshuaogwang.mzalendopos.dto.SaleItemRequest;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.service.SaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    @Autowired
    private SaleService saleService;

    @PostMapping
    public ResponseEntity<Sale> openSale(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.openSale(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<Page<Sale>> getSalesByCashier(
            @PathVariable Long cashierId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(saleService.getSalesByCashier(cashierId, pageable));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<SaleItem> addItem(
            @PathVariable Long id,
            @Valid @RequestBody SaleItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<SaleItem> updateItemQuantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(saleService.updateItemQuantity(id, itemId, quantity));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        saleService.removeItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<Sale> checkout(
            @PathVariable Long id,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(saleService.checkout(id, request));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<Sale> voidSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.voidSale(id));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getReceipt(id));
    }
}
