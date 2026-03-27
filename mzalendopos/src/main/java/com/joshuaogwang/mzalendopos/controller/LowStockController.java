package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.entity.LowStockAlert;
import com.joshuaogwang.mzalendopos.service.LowStockService;

@RestController
@RequestMapping("/api/v1/low-stock")
public class LowStockController {

    @Autowired
    private LowStockService lowStockService;

    @GetMapping("/alerts")
    public ResponseEntity<Page<LowStockAlert>> getUnresolvedAlerts(Pageable pageable) {
        return ResponseEntity.ok(lowStockService.getUnresolvedAlerts(pageable));
    }

    @GetMapping("/alerts/product/{productId}")
    public ResponseEntity<List<LowStockAlert>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(lowStockService.getAlertsByProduct(productId));
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<LowStockAlert> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(lowStockService.resolveAlert(id));
    }

    @PostMapping("/check")
    public ResponseEntity<List<LowStockAlert>> checkAlerts() {
        return ResponseEntity.ok(lowStockService.checkAndCreateAlerts());
    }
}
