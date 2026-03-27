package com.joshuaogwang.mzalendopos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;

@Data
public class ExpenseRequest {

    @NotNull(message = "Expense category is required")
    private ExpenseCategory category;

    @Positive(message = "Expense amount must be positive")
    private double amount;

    private String description;

    private String receiptReference;
}
