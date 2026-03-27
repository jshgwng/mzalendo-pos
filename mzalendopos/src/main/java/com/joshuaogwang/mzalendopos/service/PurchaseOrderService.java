package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.PurchaseOrderRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiveStockRequest;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrder;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrderStatus;

public interface PurchaseOrderService {
    PurchaseOrder createOrder(PurchaseOrderRequest request, String createdByUsername);
    PurchaseOrder sendOrder(Long id);
    PurchaseOrder receiveStock(Long id, ReceiveStockRequest request);
    PurchaseOrder cancelOrder(Long id);
    PurchaseOrder getOrderById(Long id);
    Page<PurchaseOrder> getAllOrders(PurchaseOrderStatus status, Pageable pageable);
    Page<PurchaseOrder> getOrdersBySupplier(Long supplierId, Pageable pageable);
}
