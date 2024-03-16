package com.joshuaogwang.mzalendopos.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String statusCode;
    private String message;

    public ErrorResponse(String message) {
        super();
        this.message = message;
    }
}
