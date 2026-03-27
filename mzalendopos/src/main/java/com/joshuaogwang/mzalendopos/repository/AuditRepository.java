package com.joshuaogwang.mzalendopos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joshuaogwang.mzalendopos.entity.Audit;

public interface AuditRepository extends JpaRepository<Audit, Long>{
    
}
