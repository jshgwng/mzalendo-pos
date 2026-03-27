package com.joshuaogwang.mzalendopos.entity;

public enum CreditTransactionType {
    CHARGE,   // sale posted to account
    PAYMENT,  // customer pays down balance
    ADJUSTMENT
}
