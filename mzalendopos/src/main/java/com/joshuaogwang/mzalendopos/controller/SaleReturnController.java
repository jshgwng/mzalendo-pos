package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.joshuaogwang.mzalendopos.dto.ReturnRequest;
import com.joshuaogwang.mzalendopos.entity.SaleReturn;
import com.joshuaogwang.mzalendopos.service.SaleReturnService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/returns")
public class SaleReturnController {

    @Autowired
    private SaleReturnService saleReturnService;

    @PostMapping
    public ResponseEntity<SaleReturn> processReturn(
            @Valid @RequestBody ReturnRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleReturnService.processReturn(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleReturn> getReturnById(@PathVariable Long id) {
        return ResponseEntity.ok(saleReturnService.getReturnById(id));
    }

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<SaleReturn>> getReturnsByOriginalSale(@PathVariable Long saleId) {
        return ResponseEntity.ok(saleReturnService.getReturnsByOriginalSale(saleId));
    }
}
