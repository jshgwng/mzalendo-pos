package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.entity.Role;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.RoleRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AuditService;
import com.joshuaogwang.mzalendopos.service.RoleService;

import jakarta.annotation.PostConstruct;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void init() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            roleRepository.save(adminRole);
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @Override
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new NoSuchElementException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    @Override
    public Page<Role> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }

    @Override
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Role not found with id: " + id));
    }

    @Override
    public Role saveRole(Role role, Long userId) {
        Role newRole = roleRepository.save(role);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        Audit audit = new Audit();
        audit.setEntityName("Role");
        audit.setEntityId(newRole.getId().toString());
        audit.setAction("CREATE");
        audit.setOldData("null");
        audit.setNewData(newRole.toString());
        audit.setTimeStamp(LocalDateTime.now());
        audit.setUser(user);
        auditService.createAudit(audit);

        return newRole;
    }

    @Override
    public Role updateRole(Long id, Role role) {
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Role not found with id: " + id));
        existing.setName(role.getName());
        if (role.getPermissions() != null) {
            existing.setPermissions(role.getPermissions());
        }
        return roleRepository.save(existing);
    }
}
