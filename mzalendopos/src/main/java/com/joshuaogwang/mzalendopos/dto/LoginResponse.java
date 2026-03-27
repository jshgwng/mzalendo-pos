package com.joshuaogwang.mzalendopos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private long expiresIn;
}
