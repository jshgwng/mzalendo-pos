package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.dto.DailySummaryResponse;
import com.joshuaogwang.mzalendopos.dto.TopProductResponse;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.repository.SaleItemRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Override
    public DailySummaryResponse getDailySummary(LocalDate date) {
        return buildSummary(date, date.plusDays(1));
    }

    @Override
    public DailySummaryResponse getRangeSummary(LocalDate from, LocalDate to) {
        return buildSummary(from, to.plusDays(1));
    }

    @Override
    public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = saleItemRepository.findTopProductsBetween(
                start, end, PageRequest.of(0, limit));

        return rows.stream()
                .map(row -> new TopProductResponse(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).doubleValue()))
                .collect(Collectors.toList());
    }

    private DailySummaryResponse buildSummary(LocalDate from, LocalDate exclusiveEnd) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = exclusiveEnd.atStartOfDay();

        List<Sale> completed = saleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED, start, end);

        double totalRevenue = completed.stream().mapToDouble(Sale::getTotalAmount).sum();
        double totalTax = completed.stream().mapToDouble(Sale::getTaxAmount).sum();
        double totalDiscount = completed.stream().mapToDouble(Sale::getDiscountAmount).sum();
        double totalNet = totalRevenue - totalTax;

        return DailySummaryResponse.builder()
                .date(from)
                .totalTransactions(completed.size())
                .totalRevenue(round(totalRevenue))
                .totalTax(round(totalTax))
                .totalDiscount(round(totalDiscount))
                .totalNet(round(totalNet))
                .build();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
