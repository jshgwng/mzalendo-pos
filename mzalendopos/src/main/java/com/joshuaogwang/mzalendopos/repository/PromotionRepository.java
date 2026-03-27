package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.Promotion;
import com.joshuaogwang.mzalendopos.entity.PromotionStatus;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByStatus(PromotionStatus status);

    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' " +
           "AND (p.startsAt IS NULL OR p.startsAt <= :now) " +
           "AND (p.endsAt IS NULL OR p.endsAt >= :now) " +
           "AND (p.usageLimit = 0 OR p.usageCount < p.usageLimit)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    Page<Promotion> findAll(Pageable pageable);
}
