package com.joshuaogwang.mzalendopos.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.PurchaseOrderRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiveStockRequest;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrder;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrderStatus;
import com.joshuaogwang.mzalendopos.service.PurchaseOrderService;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrder> createOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return ResponseEntity.ok(purchaseOrderService.createOrder(request, username));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<PurchaseOrder> sendOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.sendOrder(id));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrder> receiveStock(@PathVariable Long id, @Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.ok(purchaseOrderService.receiveStock(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseOrder>> getAllOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getAllOrders(status, pageable));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<Page<PurchaseOrder>> getBySupplier(@PathVariable Long supplierId, Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getOrdersBySupplier(supplierId, pageable));
    }
}
