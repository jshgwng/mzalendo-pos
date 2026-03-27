package com.joshuaogwang.mzalendopos.service;

public interface ReceiptPrintingService {

    String generateTextReceipt(Long saleId);

    byte[] generateEscPosReceipt(Long saleId);

    String generateHtmlReceipt(Long saleId);
}
