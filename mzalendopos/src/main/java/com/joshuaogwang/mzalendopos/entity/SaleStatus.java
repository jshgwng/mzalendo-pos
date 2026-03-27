package com.joshuaogwang.mzalendopos.entity;

public enum SaleStatus {
    OPEN,
    COMPLETED,
    VOIDED,
    RETURNED,
    PARTIALLY_RETURNED,
    /** Sale on hold (layaway) — items reserved, deposit optionally paid */
    HOLD
}
