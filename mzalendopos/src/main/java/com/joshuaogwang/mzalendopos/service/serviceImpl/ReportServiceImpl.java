package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.dto.DailySummaryResponse;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private SaleRepository saleRepository;

    @Override
    public DailySummaryResponse getDailySummary(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Sale> completedSales = saleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED, startOfDay, endOfDay);

        double totalRevenue = completedSales.stream()
                .mapToDouble(Sale::getTotalAmount)
                .sum();

        double totalTax = completedSales.stream()
                .mapToDouble(Sale::getTaxAmount)
                .sum();

        double totalNet = totalRevenue - totalTax;

        return DailySummaryResponse.builder()
                .date(date)
                .totalTransactions(completedSales.size())
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .totalTax(Math.round(totalTax * 100.0) / 100.0)
                .totalNet(Math.round(totalNet * 100.0) / 100.0)
                .build();
    }
}
