package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.User;

public interface UserService {
    void deleteUser(Long id);

    Page<User> getAllUsers(Pageable pageable);

    User getUserById(Long id);

    User saveUser(User user);

    User updateUser(Long id, User user);
}
