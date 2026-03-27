package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.PurchaseOrder;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrderStatus;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
    Page<PurchaseOrder> findBySupplier_Id(Long supplierId, Pageable pageable);
    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);
    Page<PurchaseOrder> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
