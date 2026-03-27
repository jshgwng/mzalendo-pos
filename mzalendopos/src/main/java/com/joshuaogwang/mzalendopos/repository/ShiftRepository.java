package com.joshuaogwang.mzalendopos.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.Shift;
import com.joshuaogwang.mzalendopos.entity.ShiftStatus;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByCashierIdAndStatus(Long cashierId, ShiftStatus status);
    Page<Shift> findByCashierId(Long cashierId, Pageable pageable);
}
