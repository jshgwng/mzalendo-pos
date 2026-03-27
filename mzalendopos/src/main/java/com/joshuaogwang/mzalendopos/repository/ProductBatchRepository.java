package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.ProductBatch;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByProductIdOrderByExpiryDateAsc(Long productId);

    List<ProductBatch> findByProductIdAndRemainingQuantityGreaterThan(Long productId, int qty);

    List<ProductBatch> findByExpiryDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT b FROM ProductBatch b WHERE b.expiryDate <= :cutoff AND b.remainingQuantity > 0")
    List<ProductBatch> findExpiringSoon(@Param("cutoff") LocalDate cutoff);
}
