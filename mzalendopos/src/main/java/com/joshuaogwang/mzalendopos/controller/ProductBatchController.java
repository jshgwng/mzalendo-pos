package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.ExpiryAlertResponse;
import com.joshuaogwang.mzalendopos.dto.ProductBatchRequest;
import com.joshuaogwang.mzalendopos.entity.ProductBatch;
import com.joshuaogwang.mzalendopos.service.ProductBatchService;

@RestController
@RequestMapping("/api/v1/batches")
public class ProductBatchController {

    @Autowired
    private ProductBatchService batchService;

    @PostMapping
    public ResponseEntity<ProductBatch> addBatch(@Valid @RequestBody ProductBatchRequest request) {
        ProductBatch batch = batchService.addBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductBatch>> getBatchesByProduct(@PathVariable Long productId) {
        List<ProductBatch> batches = batchService.getBatchesByProduct(productId);
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiryAlertResponse>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<ExpiryAlertResponse> alerts = batchService.getExpiringSoon(daysAhead);
        return ResponseEntity.ok(alerts);
    }
}
