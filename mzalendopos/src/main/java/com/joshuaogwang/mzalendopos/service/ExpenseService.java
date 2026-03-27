package com.joshuaogwang.mzalendopos.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.ExpenseRequest;
import com.joshuaogwang.mzalendopos.entity.Expense;
import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;

public interface ExpenseService {
    Expense recordExpense(ExpenseRequest request, Long shiftId, String username);
    List<Expense> getExpensesByShift(Long shiftId);
    List<Expense> getExpensesByDateRange(LocalDateTime from, LocalDateTime to);
    Page<Expense> getExpensesByCategory(ExpenseCategory category, Pageable pageable);
    double getTotalExpensesForShift(Long shiftId);
}
