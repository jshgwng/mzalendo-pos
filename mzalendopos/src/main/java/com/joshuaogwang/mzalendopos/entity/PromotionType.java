package com.joshuaogwang.mzalendopos.entity;

public enum PromotionType {
    PERCENTAGE_OFF,       // X% off the cart total
    FIXED_AMOUNT_OFF,     // Fixed UGX amount off
    BUY_X_GET_Y_FREE,     // Buy X qty of a product, get Y free
    SPEND_X_GET_Y_OFF     // Spend >= X, get Y off
}
