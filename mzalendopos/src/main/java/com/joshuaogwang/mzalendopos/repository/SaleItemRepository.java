package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.SaleItem;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySaleId(Long saleId);
    Optional<SaleItem> findBySaleIdAndProductId(Long saleId, Long productId);

    @Query("SELECT si.productName, SUM(si.quantity), SUM(si.lineTotal) " +
           "FROM SaleItem si JOIN si.sale s " +
           "WHERE s.status = 'COMPLETED' AND s.completedAt BETWEEN :from AND :to " +
           "GROUP BY si.productName ORDER BY SUM(si.quantity) DESC")
    List<Object[]> findTopProductsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
