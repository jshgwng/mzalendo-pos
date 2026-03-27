package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.Role;

public interface RoleService {
    Page<Role> getAllRoles(Pageable pageable);

    Role getRoleById(Long id);

    Role saveRole(Role role, Long userId);

    Role updateRole(Long id, Role role);

    void deleteRole(Long id);
}
