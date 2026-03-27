package com.joshuaogwang.mzalendopos.service;

import com.joshuaogwang.mzalendopos.dto.LoginRequest;
import com.joshuaogwang.mzalendopos.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
