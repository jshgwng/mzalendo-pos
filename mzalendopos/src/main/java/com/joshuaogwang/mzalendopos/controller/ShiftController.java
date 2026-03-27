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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @PostMapping("/open")
    public ResponseEntity<Shift> openShift(
            @Valid @RequestBody ShiftOpenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shiftService.openShift(userDetails.getUsername(), request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Shift> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftCloseRequest request) {
        return ResponseEntity.ok(shiftService.closeShift(id, request));
    }

    @GetMapping("/active")
    public ResponseEntity<Shift> getActiveShift(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shiftService.getActiveShift(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getShiftById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @GetMapping
    public ResponseEntity<Page<Shift>> getAllShifts(
            @RequestParam(required = false) Long cashierId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (cashierId != null) {
            return ResponseEntity.ok(shiftService.getShiftsByCashier(cashierId, pageable));
        }
        return ResponseEntity.ok(shiftService.getAllShifts(pageable));
    }
}
