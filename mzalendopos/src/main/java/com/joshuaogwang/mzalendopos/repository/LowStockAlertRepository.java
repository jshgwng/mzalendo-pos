package com.joshuaogwang.mzalendopos.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.LowStockAlert;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {
    List<LowStockAlert> findByResolvedFalse();
    List<LowStockAlert> findByProductId(Long productId);
    Page<LowStockAlert> findByResolvedFalse(Pageable pageable);
}
