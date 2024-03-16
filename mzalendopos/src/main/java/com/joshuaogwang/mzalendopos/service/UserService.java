package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.User;

public interface UserService {
    public void deleteUser(Long id);

    public List<User> getAllUsers();

    public User getUserById(Long id);

    public User saveUser(User user);

    public User updateUser(User user);
}
