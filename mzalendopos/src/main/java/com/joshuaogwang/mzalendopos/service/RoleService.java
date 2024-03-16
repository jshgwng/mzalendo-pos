package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.Role;

public interface RoleService {
    public List<Role> getAllRoles();

    public Role getRoleById(Long id);

    public Role saveRole(Role role, Long userId);

    public Role updateRole(Role role);

    public void deleteRole(Long id);
}
