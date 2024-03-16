package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.AuditRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AuditService;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public List<Audit> getAllAudits() {
        return auditRepository.findAll();
    }

    @Override
    public Audit getAuditById(Long id) {
        return auditRepository.findById(id).get();
    }

    @Override
    public Audit createAudit(Audit audit) {
        User user = userRepository.findById(audit.getUser().getId()).get();
        if (user != null) {
            audit.setUser(user);
        }
        return auditRepository.save(audit);
    }

    @Override
    public Audit updateAudit(Audit audit) {
        return auditRepository.save(audit);
    }

    @Override
    public void deleteAudit(Long id) {
        auditRepository.deleteById(id);
    }

}
