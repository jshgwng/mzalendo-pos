package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.Audit;

public interface AuditService {
    Page<Audit> getAllAudits(Pageable pageable);

    Audit getAuditById(Long id);

    Audit createAudit(Audit audit);

    Audit updateAudit(Long id, Audit audit);

    void deleteAudit(Long id);
}
