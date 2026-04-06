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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.ShiftCloseRequest;
import com.joshuaogwang.mzalendopos.dto.ShiftOpenRequest;
import com.joshuaogwang.mzalendopos.entity.Shift;
import com.joshuaogwang.mzalendopos.service.ShiftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shifts")
@Tag(name = "Shifts", description = "Manage cashier shifts including open, close, and shift history")
@SecurityRequirement(name = "bearerAuth")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @PostMapping("/open")
    @Operation(summary = "Open shift", description = "Opens a new shift for the authenticated cashier with an opening cash amount")
    @ApiResponse(responseCode = "201", description = "Shift opened")
    @ApiResponse(responseCode = "400", description = "Cashier already has an open shift")
    public ResponseEntity<Shift> openShift(
            @Valid @RequestBody ShiftOpenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shiftService.openShift(userDetails.getUsername(), request));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close shift", description = "Closes the specified shift with a closing cash count and notes")
    @ApiResponse(responseCode = "200", description = "Shift closed")
    @ApiResponse(responseCode = "404", description = "Shift not found")
    public ResponseEntity<Shift> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftCloseRequest request) {
        return ResponseEntity.ok(shiftService.closeShift(id, request));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active shift", description = "Returns the currently open shift for the authenticated cashier")
    @ApiResponse(responseCode = "200", description = "Active shift found")
    @ApiResponse(responseCode = "404", description = "No active shift for this cashier")
    public ResponseEntity<Shift> getActiveShift(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shiftService.getActiveShift(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shift by ID")
    @ApiResponse(responseCode = "200", description = "Shift found")
    @ApiResponse(responseCode = "404", description = "Shift not found")
    public ResponseEntity<Shift> getShiftById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @GetMapping
    @Operation(summary = "List shifts", description = "Returns a paginated list of all shifts, optionally filtered by cashier")
    @ApiResponse(responseCode = "200", description = "Shifts retrieved successfully")
    public ResponseEntity<Page<Shift>> getAllShifts(
            @Parameter(description = "Filter by cashier ID") @RequestParam(required = false) Long cashierId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (cashierId != null) {
            return ResponseEntity.ok(shiftService.getShiftsByCashier(cashierId, pageable));
        }
        return ResponseEntity.ok(shiftService.getAllShifts(pageable));
    }
}
