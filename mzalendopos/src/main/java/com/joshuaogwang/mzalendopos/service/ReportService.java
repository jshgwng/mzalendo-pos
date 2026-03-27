package com.joshuaogwang.mzalendopos.service;

import java.time.LocalDate;

import com.joshuaogwang.mzalendopos.dto.DailySummaryResponse;

public interface ReportService {
    DailySummaryResponse getDailySummary(LocalDate date);
}
