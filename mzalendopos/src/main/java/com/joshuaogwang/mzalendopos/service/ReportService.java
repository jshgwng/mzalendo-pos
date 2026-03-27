package com.joshuaogwang.mzalendopos.service;

import java.time.LocalDate;
import java.util.List;

import com.joshuaogwang.mzalendopos.dto.DailySummaryResponse;
import com.joshuaogwang.mzalendopos.dto.TopProductResponse;

public interface ReportService {
    DailySummaryResponse getDailySummary(LocalDate date);
    DailySummaryResponse getRangeSummary(LocalDate from, LocalDate to);
    List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit);
}
