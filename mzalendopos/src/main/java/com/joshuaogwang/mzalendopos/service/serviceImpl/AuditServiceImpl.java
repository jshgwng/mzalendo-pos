package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.repository.AuditRepository;
import com.joshuaogwang.mzalendopos.service.AuditService;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditRepository auditRepository;

    @Override
    public Page<Audit> getAllAudits(Pageable pageable) {
        return auditRepository.findAll(pageable);
    }

    @Override
    public Audit getAuditById(Long id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Audit not found with id: " + id));
    }

    @Override
    public Audit createAudit(Audit audit) {
        audit.setTimeStamp(LocalDateTime.now());
        return auditRepository.save(audit);
    }

    @Override
    public Audit updateAudit(Long id, Audit audit) {
        Audit existing = auditRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Audit not found with id: " + id));
        existing.setEntityName(audit.getEntityName());
        existing.setEntityId(audit.getEntityId());
        existing.setAction(audit.getAction());
        existing.setOldData(audit.getOldData());
        existing.setNewData(audit.getNewData());
        return auditRepository.save(existing);
    }

    @Override
    public void deleteAudit(Long id) {
        if (!auditRepository.existsById(id)) {
            throw new NoSuchElementException("Audit not found with id: " + id);
        }
        auditRepository.deleteById(id);
    }
}
