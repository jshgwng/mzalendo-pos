package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ShiftCloseRequest;
import com.joshuaogwang.mzalendopos.dto.ShiftOpenRequest;
import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.joshuaogwang.mzalendopos.entity.Shift;
import com.joshuaogwang.mzalendopos.entity.ShiftStatus;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.repository.ShiftRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.ShiftService;

@Service
public class ShiftServiceImpl implements ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Shift openShift(String cashierUsername, ShiftOpenRequest request) {
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierUsername));

        shiftRepository.findByCashierIdAndStatus(cashier.getId(), ShiftStatus.OPEN)
                .ifPresent(s -> { throw new IllegalArgumentException("You already have an open shift (ID: " + s.getId() + ")"); });

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
                .orElseThrow(() -> new NoSuchElementException("Shift not found with id: " + shiftId));

        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new IllegalArgumentException("Shift is already closed");
        }

        LocalDateTime closedAt = LocalDateTime.now();

        // Calculate expected cash: opening cash + net cash sales during this shift
        List<Payment> cashPayments = paymentRepository.findByCashierAndMethodAndDateRange(
                shift.getCashier().getId(), PaymentMethod.CASH, shift.getOpenedAt(), closedAt);

        double totalCashSales = cashPayments.stream()
                .mapToDouble(p -> p.getAmountPaid() - p.getChangeGiven())
                .sum();

        double expectedCash = shift.getOpeningCash() + totalCashSales;
        double variance = request.getClosingCash() - expectedCash;

        shift.setClosingCash(request.getClosingCash());
        shift.setExpectedCash(Math.round(expectedCash * 100.0) / 100.0);
        shift.setCashVariance(Math.round(variance * 100.0) / 100.0);
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
                .orElseThrow(() -> new NoSuchElementException("No active shift found for user: " + cashierUsername));
    }

    @Override
    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Shift not found with id: " + id));
    }

    @Override
    public Page<Shift> getAllShifts(Pageable pageable) {
        return shiftRepository.findAll(pageable);
    }

    @Override
    public Page<Shift> getShiftsByCashier(Long cashierId, Pageable pageable) {
        return shiftRepository.findByCashierId(cashierId, pageable);
    }
}
