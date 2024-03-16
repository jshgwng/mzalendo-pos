package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Role;
import com.joshuaogwang.mzalendopos.repository.RoleRepository;
import com.joshuaogwang.mzalendopos.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

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
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Role role) {
        // TODO Auto-generated method stub
        return null;
    }

}
