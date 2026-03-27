package com.joshuaogwang.mzalendopos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.SaleItem;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySaleId(Long saleId);
    Optional<SaleItem> findBySaleIdAndProductId(Long saleId, Long productId);
}
