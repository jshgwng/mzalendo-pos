package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.StockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.entity.StockAdjustment;

public interface InventoryService {
    StockAdjustment adjustStock(StockAdjustmentRequest request, String username);
    Page<StockAdjustment> getAllAdjustments(Pageable pageable);
    Page<StockAdjustment> getAdjustmentsByProduct(Long productId, Pageable pageable);
}
