package com.joshuaogwang.mzalendopos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joshuaogwang.mzalendopos.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{
    
}
