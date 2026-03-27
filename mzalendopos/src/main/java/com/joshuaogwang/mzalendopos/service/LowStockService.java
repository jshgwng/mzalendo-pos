package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.LowStockAlert;

public interface LowStockService {
    List<LowStockAlert> checkAndCreateAlerts();
    Page<LowStockAlert> getUnresolvedAlerts(Pageable pageable);
    List<LowStockAlert> getAlertsByProduct(Long productId);
    LowStockAlert resolveAlert(Long alertId);
}
