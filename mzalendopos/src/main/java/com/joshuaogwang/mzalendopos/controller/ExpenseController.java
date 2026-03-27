package com.joshuaogwang.mzalendopos.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.ExpenseRequest;
import com.joshuaogwang.mzalendopos.entity.Expense;
import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;
import com.joshuaogwang.mzalendopos.service.ExpenseService;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<Expense> recordExpense(
            @Valid @RequestBody ExpenseRequest request,
            @RequestParam(required = false) Long shiftId) {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return ResponseEntity.ok(expenseService.recordExpense(request, shiftId, username));
    }

    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<Expense>> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(expenseService.getExpensesByShift(shiftId));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(from, to));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Expense>> getByCategory(@PathVariable ExpenseCategory category, Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(category, pageable));
    }
}
