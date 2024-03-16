package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.Role;

public interface RoleService {
    public List<Role> getAllRoles();

    public Role getRoleById(Long id);

    public Role saveRole(Role role);

    public Role updateRole(Role role);

    public void deleteRole(Long id);
}
