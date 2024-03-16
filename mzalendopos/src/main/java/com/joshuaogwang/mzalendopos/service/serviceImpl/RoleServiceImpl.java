package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.List;

import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.entity.Role;
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
        // TODO Auto-generated method stub

    }

    @Override
    public List<Role> getAllRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role getRoleById(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role saveRole(Role role, Long userId) {
        Role newRole = roleRepository.save(role);
        Audit audit = new Audit();
        audit.setEntityName("Role");
        audit.setEntityId(newRole.getId().toString());
        audit.setAction("CREATE");
        audit.setOldData("null");   
        audit.setNewData(newRole.toString());   
        audit.setUser(userRepository.findById(userId).get());
        auditService.createAudit(audit);
        return newRole;
    }

    @Override
    public Role updateRole(Role role) {
        // TODO Auto-generated method stub
        return null;
    }

}
