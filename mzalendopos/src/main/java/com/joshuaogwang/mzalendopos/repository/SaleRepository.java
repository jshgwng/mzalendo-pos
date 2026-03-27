package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findBySaleNumber(String saleNumber);
    Page<Sale> findByCashierId(Long cashierId, Pageable pageable);
    Page<Sale> findByStatus(SaleStatus status, Pageable pageable);
    Page<Sale> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<Sale> findByStatusAndCreatedAtBetween(SaleStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<Sale> findByStatusAndCreatedAtBetween(SaleStatus status, LocalDateTime start, LocalDateTime end);
}
