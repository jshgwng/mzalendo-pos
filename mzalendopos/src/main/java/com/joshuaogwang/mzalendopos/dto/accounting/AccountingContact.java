package com.joshuaogwang.mzalendopos.dto.accounting;

import lombok.Builder;
import lombok.Data;

/**
 * Normalized customer/contact for any accounting provider.
 * All fields are optional for walk-in (anonymous) customers.
 */
@Data
@Builder
public class AccountingContact {
    private String name;
    private String email;
    private String phone;
}
