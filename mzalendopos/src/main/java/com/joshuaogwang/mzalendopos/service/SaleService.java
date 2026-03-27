package com.joshuaogwang.mzalendopos.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.CheckoutRequest;
import com.joshuaogwang.mzalendopos.dto.DiscountRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiptResponse;
import com.joshuaogwang.mzalendopos.dto.SaleItemRequest;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;

public interface SaleService {
    Sale openSale(String cashierUsername);
    SaleItem addItem(Long saleId, SaleItemRequest request);
    SaleItem updateItemQuantity(Long saleId, Long itemId, int quantity);
    void removeItem(Long saleId, Long itemId);
    Sale applyDiscount(Long saleId, DiscountRequest request);
    Sale removeDiscount(Long saleId);
    Sale getSaleById(Long id);
    Page<Sale> getAllSales(SaleStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<Sale> getSalesByCashier(Long cashierId, Pageable pageable);
    Sale checkout(Long saleId, CheckoutRequest request);
    Sale voidSale(Long saleId);
    ReceiptResponse getReceipt(Long saleId);
}
