package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.Audit;

public interface AuditService {
    public List<Audit> getAllAudits();
    public Audit getAuditById(Long id);
    public Audit createAudit(Audit audit);
    public Audit updateAudit(Audit audit);
    public void deleteAudit(Long id);
}
