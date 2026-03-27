package com.joshuaogwang.mzalendopos.entity;

public enum PaymentMethod {
    CASH,
    CARD,
    MOBILE_MONEY,
    /** Customer account credit — debited against CustomerAccount balance */
    CREDIT
}
