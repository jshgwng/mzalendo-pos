package com.joshuaogwang.mzalendopos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.User;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
