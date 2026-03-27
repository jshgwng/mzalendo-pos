package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.Expense;
import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByShiftId(Long shiftId);
    List<Expense> findByRecordedAtBetween(LocalDateTime from, LocalDateTime to);
    Page<Expense> findByCategory(ExpenseCategory category, Pageable pageable);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.shift.id = :shiftId")
    Double sumByShiftId(@Param("shiftId") Long shiftId);
}
