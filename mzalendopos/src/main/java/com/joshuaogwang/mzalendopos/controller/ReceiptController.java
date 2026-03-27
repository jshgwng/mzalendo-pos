package com.joshuaogwang.mzalendopos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.service.ReceiptPrintingService;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    @Autowired
    private ReceiptPrintingService receiptPrintingService;

    @GetMapping("/{saleId}/text")
    public ResponseEntity<String> getTextReceipt(@PathVariable Long saleId) {
        String receipt = receiptPrintingService.generateTextReceipt(saleId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(receipt);
    }

    @GetMapping("/{saleId}/escpos")
    public ResponseEntity<byte[]> getEscPosReceipt(@PathVariable Long saleId) {
        byte[] receipt = receiptPrintingService.generateEscPosReceipt(saleId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("receipt.bin").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(receipt);
    }

    @GetMapping("/{saleId}/html")
    public ResponseEntity<String> getHtmlReceipt(@PathVariable Long saleId) {
        String receipt = receiptPrintingService.generateHtmlReceipt(saleId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(receipt);
    }
}
