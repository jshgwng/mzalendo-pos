package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ShiftCloseRequest;
import com.joshuaogwang.mzalendopos.dto.ShiftOpenRequest;
import com.joshuaogwang.mzalendopos.dto.ZReportResponse;
import com.joshuaogwang.mzalendopos.entity.Expense;
import com.joshuaogwang.mzalendopos.entity.ExpenseCategory;
import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.entity.Shift;
import com.joshuaogwang.mzalendopos.entity.ShiftStatus;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.ExpenseRepository;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.repository.ShiftRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.ShiftService;

@Service
public class ShiftServiceImpl implements ShiftService {

    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ExpenseRepository expenseRepository;

    @Override
    @Transactional
    public Shift openShift(String cashierUsername, ShiftOpenRequest request) {
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierUsername));

        shiftRepository.findByCashierIdAndStatus(cashier.getId(), ShiftStatus.OPEN)
                .ifPresent(s -> { throw new IllegalArgumentException(
                        "You already have an open shift (ID: " + s.getId() + ")"); });

        Shift shift = new Shift();
        shift.setCashier(cashier);
        shift.setOpeningCash(request.getOpeningCash());
        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpenedAt(LocalDateTime.now());
        return shiftRepository.save(shift);
    }

    @Override
    @Transactional
    public Shift closeShift(Long shiftId, ShiftCloseRequest request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NoSuchElementException("Shift not found: " + shiftId));

        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new IllegalArgumentException("Shift is already closed");
        }

        LocalDateTime closedAt = LocalDateTime.now();

        List<Payment> cashPayments = paymentRepository.findByCashierAndMethodAndDateRange(
                shift.getCashier().getId(), PaymentMethod.CASH, shift.getOpenedAt(), closedAt);

        double totalCashSales = cashPayments.stream()
                .mapToDouble(p -> p.getAmountPaid() - p.getChangeGiven())
                .sum();

        Double totalExpenses = expenseRepository.sumByShiftId(shiftId);
        double expensesTotal = totalExpenses != null ? totalExpenses : 0.0;

        double expectedCash = shift.getOpeningCash() + totalCashSales - expensesTotal;
        double variance = request.getClosingCash() - expectedCash;

        shift.setClosingCash(request.getClosingCash());
        shift.setExpectedCash(round(expectedCash));
        shift.setCashVariance(round(variance));
        shift.setClosedAt(closedAt);
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setNotes(request.getNotes());
        return shiftRepository.save(shift);
    }

    @Override
    public Shift getActiveShift(String cashierUsername) {
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierUsername));
        return shiftRepository.findByCashierIdAndStatus(cashier.getId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new NoSuchElementException("No active shift for: " + cashierUsername));
    }

    @Override
    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Shift not found: " + id));
    }

    @Override
    public Page<Shift> getAllShifts(Pageable pageable) {
        return shiftRepository.findAll(pageable);
    }

    @Override
    public Page<Shift> getShiftsByCashier(Long cashierId, Pageable pageable) {
        return shiftRepository.findByCashierId(cashierId, pageable);
    }

    @Override
    public ZReportResponse generateZReport(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NoSuchElementException("Shift not found: " + shiftId));

        LocalDateTime from = shift.getOpenedAt();
        LocalDateTime to = shift.getClosedAt() != null ? shift.getClosedAt() : LocalDateTime.now();
        Long cashierId = shift.getCashier().getId();

        // Payment breakdown by method
        double cashSales = sumNet(paymentRepository.findByCashierAndMethodAndDateRange(
                cashierId, PaymentMethod.CASH, from, to));
        double cardSales = sumNet(paymentRepository.findByCashierAndMethodAndDateRange(
                cashierId, PaymentMethod.CARD, from, to));
        double mobileSales = sumNet(paymentRepository.findByCashierAndMethodAndDateRange(
                cashierId, PaymentMethod.MOBILE_MONEY, from, to));
        double creditSales = sumNet(paymentRepository.findByCashierAndMethodAndDateRange(
                cashierId, PaymentMethod.CREDIT, from, to));

        double totalRevenue = cashSales + cardSales + mobileSales + creditSales;

        // Sales statistics
        List<Sale> completedSales = saleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED, from, to);
        // Filter to this cashier's sales
        List<Sale> cashierSales = completedSales.stream()
                .filter(s -> s.getCashier().getId().equals(cashierId))
                .collect(Collectors.toList());

        double totalTax = cashierSales.stream().mapToDouble(Sale::getTaxAmount).sum();
        double totalDiscount = cashierSales.stream()
                .mapToDouble(s -> s.getDiscountAmount() + s.getPromotionDiscount())
                .sum();

        // Expenses for this shift
        List<Expense> expenses = expenseRepository.findByShiftId(shiftId);
        double totalExpenses = expenses.stream().mapToDouble(Expense::getAmount).sum();

        // Expense breakdown by category
        Map<ExpenseCategory, Double> expenseByCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));

        List<ZReportResponse.ExpenseSummary> expenseSummaries = new ArrayList<>();
        expenseByCategory.forEach((cat, amount) ->
                expenseSummaries.add(ZReportResponse.ExpenseSummary.builder()
                        .category(cat)
                        .totalAmount(round(amount))
                        .build()));

        double netCash = shift.getOpeningCash() + cashSales - totalExpenses;

        String cashierName = shift.getCashier().getFirstName() + " " + shift.getCashier().getLastName();

        return ZReportResponse.builder()
                .shiftId(shiftId)
                .cashierName(cashierName)
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .openingCash(shift.getOpeningCash())
                .closingCash(shift.getClosingCash())
                .expectedCash(shift.getExpectedCash())
                .cashVariance(shift.getCashVariance())
                .totalTransactions(cashierSales.size())
                .totalRevenue(round(totalRevenue))
                .totalTax(round(totalTax))
                .totalDiscount(round(totalDiscount))
                .cashSales(round(cashSales))
                .cardSales(round(cardSales))
                .mobileMoneySales(round(mobileSales))
                .creditSales(round(creditSales))
                .totalExpenses(round(totalExpenses))
                .netCash(round(netCash))
                .expenses(expenseSummaries)
                .notes(shift.getNotes())
                .build();
    }

    private double sumNet(List<Payment> payments) {
        return payments.stream().mapToDouble(p -> p.getAmountPaid() - p.getChangeGiven()).sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
