package com.joshuaogwang.mzalendopos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySaleId(Long saleId);

    @Query("SELECT p FROM Payment p WHERE p.method = :method AND p.sale.cashier.id = :cashierId AND p.paidAt BETWEEN :from AND :to")
    List<Payment> findByCashierAndMethodAndDateRange(
            @Param("cashierId") Long cashierId,
            @Param("method") PaymentMethod method,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
