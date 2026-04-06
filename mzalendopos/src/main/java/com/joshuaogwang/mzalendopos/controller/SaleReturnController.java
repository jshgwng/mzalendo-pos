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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/returns")
@Tag(name = "Returns", description = "Process and view sale returns and refunds")
@SecurityRequirement(name = "bearerAuth")
public class SaleReturnController {

    @Autowired
    private SaleReturnService saleReturnService;

    @PostMapping
    @Operation(summary = "Process return", description = "Creates a return for one or more items from a completed sale and issues a refund")
    @ApiResponse(responseCode = "201", description = "Return processed")
    @ApiResponse(responseCode = "400", description = "Validation error or items not eligible for return")
    @ApiResponse(responseCode = "404", description = "Original sale not found")
    public ResponseEntity<SaleReturn> processReturn(
            @Valid @RequestBody ReturnRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleReturnService.processReturn(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return by ID")
    @ApiResponse(responseCode = "200", description = "Return found")
    @ApiResponse(responseCode = "404", description = "Return not found")
    public ResponseEntity<SaleReturn> getReturnById(@PathVariable Long id) {
        return ResponseEntity.ok(saleReturnService.getReturnById(id));
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "Get returns by sale", description = "Returns all return records associated with a given original sale")
    @ApiResponse(responseCode = "200", description = "Returns retrieved successfully")
    public ResponseEntity<List<SaleReturn>> getReturnsByOriginalSale(@PathVariable Long saleId) {
        return ResponseEntity.ok(saleReturnService.getReturnsByOriginalSale(saleId));
    }
}
