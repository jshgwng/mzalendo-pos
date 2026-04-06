package com.joshuaogwang.mzalendopos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.DailySummaryResponse;
import com.joshuaogwang.mzalendopos.dto.TopProductResponse;
import com.joshuaogwang.mzalendopos.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Sales summaries and business intelligence reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/daily-summary")
    @Operation(summary = "Daily sales summary", description = "Returns total sales, revenue, and transaction count for a given date (defaults to today)")
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<DailySummaryResponse> getDailySummary(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd). Defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailySummary(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Date range sales summary", description = "Returns aggregated sales metrics over a specified date range")
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<DailySummaryResponse> getRangeSummary(
            @Parameter(description = "Start date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getRangeSummary(from, to));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top selling products", description = "Returns the best-selling products ranked by quantity sold within a date range (defaults to current month)")
    @ApiResponse(responseCode = "200", description = "Top products retrieved successfully")
    public ResponseEntity<List<TopProductResponse>> getTopProducts(
            @Parameter(description = "Start date (yyyy-MM-dd). Defaults to start of current month.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd). Defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Number of products to return (default 10)")
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        return ResponseEntity.ok(reportService.getTopProducts(start, end, limit));
    }
}
