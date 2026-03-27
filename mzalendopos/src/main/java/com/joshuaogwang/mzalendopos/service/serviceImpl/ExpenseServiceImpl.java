package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ExpenseRequest;
import com.joshuaogwang.mzalendopos.entity.Expense;
import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;
import com.joshuaogwang.mzalendopos.entity.Shift;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.ExpenseRepository;
import com.joshuaogwang.mzalendopos.repository.ShiftRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.ExpenseService;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Expense recordExpense(ExpenseRequest request, Long shiftId, String username) {
        User recordedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        Shift shift = null;
        if (shiftId != null) {
            shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new NoSuchElementException("Shift not found with id: " + shiftId));
        }

        Expense expense = new Expense();
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setShift(shift);
        expense.setRecordedBy(recordedBy);
        expense.setRecordedAt(LocalDateTime.now());
        expense.setReceiptReference(request.getReceiptReference());

        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getExpensesByShift(Long shiftId) {
        return expenseRepository.findByShiftId(shiftId);
    }

    @Override
    public List<Expense> getExpensesByDateRange(LocalDateTime from, LocalDateTime to) {
        return expenseRepository.findByRecordedAtBetween(from, to);
    }

    @Override
    public Page<Expense> getExpensesByCategory(ExpenseCategory category, Pageable pageable) {
        return expenseRepository.findByCategory(category, pageable);
    }

    @Override
    public double getTotalExpensesForShift(Long shiftId) {
        Double total = expenseRepository.sumByShiftId(shiftId);
        return total != null ? total : 0.0;
    }
}
