package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.ShiftCloseRequest;
import com.joshuaogwang.mzalendopos.dto.ShiftOpenRequest;
import com.joshuaogwang.mzalendopos.dto.ZReportResponse;
import com.joshuaogwang.mzalendopos.entity.Shift;

public interface ShiftService {
    Shift openShift(String cashierUsername, ShiftOpenRequest request);
    Shift closeShift(Long shiftId, ShiftCloseRequest request);
    Shift getActiveShift(String cashierUsername);
    Shift getShiftById(Long id);
    Page<Shift> getAllShifts(Pageable pageable);
    Page<Shift> getShiftsByCashier(Long cashierId, Pageable pageable);
    ZReportResponse generateZReport(Long shiftId);
}
