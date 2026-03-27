package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.CheckoutRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiptResponse;
import com.joshuaogwang.mzalendopos.dto.SaleItemRequest;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;

public interface SaleService {
    Sale openSale(String cashierUsername);
    SaleItem addItem(Long saleId, SaleItemRequest request);
    SaleItem updateItemQuantity(Long saleId, Long itemId, int quantity);
    void removeItem(Long saleId, Long itemId);
    Sale getSaleById(Long id);
    Page<Sale> getSalesByCashier(Long cashierId, Pageable pageable);
    Sale checkout(Long saleId, CheckoutRequest request);
    Sale voidSale(Long saleId);
    ReceiptResponse getReceipt(Long saleId);
}
