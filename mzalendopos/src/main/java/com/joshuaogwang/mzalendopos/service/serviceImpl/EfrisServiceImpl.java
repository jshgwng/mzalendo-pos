package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshuaogwang.mzalendopos.config.EfrisProperties;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisApiRequest;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisApiResponse;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisDataDescription;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisGlobalInfo;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisInvoiceContent;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisInvoiceResult;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisRequestData;
import com.joshuaogwang.mzalendopos.dto.efris.EfrisReturnStateInfo;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmissionStatus;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.repository.EfrisSubmissionRepository;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.service.EfrisService;

@Service
public class EfrisServiceImpl implements EfrisService {

    private static final Logger log = LoggerFactory.getLogger(EfrisServiceImpl.class);

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final DateTimeFormatter EFRIS_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private EfrisProperties efrisProperties;

    @Autowired
    private EfrisSubmissionRepository submissionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public EfrisSubmission submitInvoice(Sale sale) {
        EfrisSubmission submission = submissionRepository.findBySaleId(sale.getId())
                .orElseGet(() -> {
                    EfrisSubmission s = new EfrisSubmission();
                    s.setSale(sale);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        if (!efrisProperties.isEnabled()) {
            submission.setStatus(EfrisSubmissionStatus.PENDING);
            submission.setErrorMessage("EFRIS integration is disabled");
            return submissionRepository.save(submission);
        }

        return attemptSubmission(submission, sale);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${efris.retry-interval-ms:300000}")
    public List<EfrisSubmission> retryPendingSubmissions() {
        List<EfrisSubmission> pending = submissionRepository
                .findByStatusAndAttemptCountLessThan(
                        EfrisSubmissionStatus.PENDING, efrisProperties.getMaxRetryAttempts());
        pending.addAll(submissionRepository
                .findByStatusAndAttemptCountLessThan(
                        EfrisSubmissionStatus.RETRY, efrisProperties.getMaxRetryAttempts()));

        return pending.stream()
                .map(s -> attemptSubmission(s, s.getSale()))
                .collect(Collectors.toList());
    }

    @Override
    public EfrisSubmission getSubmissionBySaleId(Long saleId) {
        return submissionRepository.findBySaleId(saleId).orElse(null);
    }

    // -------------------------------------------------------------------------
    // Core submission logic
    // -------------------------------------------------------------------------

    private EfrisSubmission attemptSubmission(EfrisSubmission submission, Sale sale) {
        submission.setAttemptCount(submission.getAttemptCount() + 1);
        submission.setLastAttemptAt(LocalDateTime.now());

        try {
            EfrisInvoiceContent content = buildInvoiceContent(sale);
            String encryptedContent = encryptContent(content);
            EfrisApiRequest request = buildApiRequest(encryptedContent, sale.getSaleNumber());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EfrisApiRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<EfrisApiResponse> response = restTemplate.postForEntity(
                    efrisProperties.getEffectiveApiUrl(),
                    entity,
                    EfrisApiResponse.class);

            EfrisApiResponse body = response.getBody();
            if (body == null) {
                return markFailed(submission, "Empty response from URA EFRIS", null);
            }

            EfrisReturnStateInfo state = body.getReturnStateInfo();
            if (state != null && !"00".equals(state.getReturnCode())) {
                return markFailed(submission,
                        "URA error: " + state.getReturnMessage(), state.getReturnCode());
            }

            if (body.getData() == null || body.getData().getContent() == null) {
                return markFailed(submission, "URA returned empty data content", null);
            }

            EfrisInvoiceResult result = decryptResponse(body.getData().getContent());
            return markSubmitted(submission, result, state != null ? state.getReturnCode() : "00");

        } catch (Exception ex) {
            log.error("EFRIS submission failed for sale {}: {}", sale.getSaleNumber(), ex.getMessage(), ex);
            submission.setStatus(EfrisSubmissionStatus.RETRY);
            submission.setErrorMessage(ex.getMessage());
            return submissionRepository.save(submission);
        }
    }

    // -------------------------------------------------------------------------
    // Invoice content builder
    // -------------------------------------------------------------------------

    private EfrisInvoiceContent buildInvoiceContent(Sale sale) {
        var payment = paymentRepository.findBySaleId(sale.getId()).orElse(null);

        List<EfrisInvoiceContent.GoodsDetail> goods = sale.getItems().stream()
                .map(item -> buildGoodsDetail(item, sale.getItems().indexOf(item) + 1))
                .collect(Collectors.toList());

        // Aggregate tax by rate
        double totalNet = sale.getSubtotal();
        double totalTax = sale.getTaxAmount();
        double gross = sale.getTotalAmount();

        List<EfrisInvoiceContent.TaxDetail> taxDetails = sale.getItems().stream()
                .collect(Collectors.groupingBy(
                        i -> String.valueOf(i.getTaxRate()),
                        Collectors.summingDouble(i -> i.getLineTotal() * (i.getTaxRate() / 100.0))))
                .entrySet().stream()
                .map(e -> EfrisInvoiceContent.TaxDetail.builder()
                        .taxCategory("101")
                        .taxRate(e.getKey())
                        .netAmount(String.format("%.2f", totalNet))
                        .taxAmount(String.format("%.2f", e.getValue()))
                        .build())
                .collect(Collectors.toList());

        String cashierName = sale.getCashier() != null
                ? sale.getCashier().getFirstName() + " " + sale.getCashier().getLastName()
                : "Cashier";

        EfrisInvoiceContent.BuyerDetails buyerDetails = buildBuyerDetails(sale);

        return EfrisInvoiceContent.builder()
                .sellerDetails(EfrisInvoiceContent.SellerDetails.builder()
                        .tin(efrisProperties.getTin())
                        .ninBrn("")
                        .legalName(efrisProperties.getLegalName())
                        .businessName(efrisProperties.getBusinessName())
                        .address(efrisProperties.getAddress())
                        .mobilePhone(efrisProperties.getMobilePhone())
                        .linePhone("")
                        .emailAddress("")
                        .placeOfBusiness(efrisProperties.getPlaceOfBusiness())
                        .referenceNo("")
                        .isCheckReferenceNo("0")
                        .build())
                .basicInformation(EfrisInvoiceContent.BasicInformation.builder()
                        .invoiceNo(sale.getSaleNumber())
                        .antifakeCode("")
                        .deviceNo(efrisProperties.getDeviceNo())
                        .issuedDate(sale.getCompletedAt().format(EFRIS_DATE_FMT))
                        .operator(cashierName)
                        .currency("UGX")
                        .localCurrencyRate("1")
                        .invoiceType("1")
                        .invoiceKind("1")
                        .dataSource("103")
                        .invoiceIndustryCode(efrisProperties.getIndustryCode())
                        .isBatch("0")
                        .build())
                .buyerDetails(buyerDetails)
                .goodsDetails(goods)
                .taxDetails(taxDetails)
                .summaryDetails(EfrisInvoiceContent.SummaryDetails.builder()
                        .netAmount(String.format("%.2f", totalNet))
                        .taxAmount(String.format("%.2f", totalTax))
                        .grossAmount(String.format("%.2f", gross))
                        .itemCount(String.valueOf(goods.size()))
                        .modeCode("0")
                        .remarks("")
                        .qrCode("")
                        .build())
                .payWay(List.of(EfrisInvoiceContent.PayWay.builder()
                        .paymentMode(toEfrisPaymentMode(
                                payment != null ? payment.getMethod() : PaymentMethod.CASH))
                        .paymentAmount(String.format("%.2f", gross))
                        .orderNumber("")
                        .build()))
                .build();
    }

    private EfrisInvoiceContent.GoodsDetail buildGoodsDetail(SaleItem item, int orderNumber) {
        double taxRate = item.getTaxRate();
        double lineNet = item.getLineTotal();
        double lineTax = lineNet * (taxRate / 100.0);

        return EfrisInvoiceContent.GoodsDetail.builder()
                .item(item.getProductName())
                .itemCode(item.getProduct() != null && item.getProduct().getBarcode() != null
                        ? item.getProduct().getBarcode() : "")
                .barCode(item.getProduct() != null && item.getProduct().getBarcode() != null
                        ? item.getProduct().getBarcode() : "")
                .measure("EA")
                .quantity(String.valueOf(item.getQuantity()))
                .unitPrice(String.format("%.2f", item.getUnitPrice()))
                .total(String.format("%.2f", lineNet))
                .taxRate(String.format("%.0f", taxRate))
                .tax(String.format("%.2f", lineTax))
                .orderNumber(String.valueOf(orderNumber))
                .goodsCategoryId("101")
                .goodsCategoryName("General Goods")
                .serviceType("")
                .discountTaxRate("0")
                .discountTotal("0.00")
                .discountFlag("0")
                .build();
    }

    private EfrisInvoiceContent.BuyerDetails buildBuyerDetails(Sale sale) {
        if (sale.getCustomer() != null) {
            var c = sale.getCustomer();
            return EfrisInvoiceContent.BuyerDetails.builder()
                    .buyerTin("")
                    .buyerNinBrn("")
                    .buyerPassportNum("")
                    .buyerLegalName(c.getName())
                    .buyerBusinessName("")
                    .buyerAddress("")
                    .buyerEmail(c.getEmail() != null ? c.getEmail() : "")
                    .buyerMobilePhone(c.getPhoneNumber() != null ? c.getPhoneNumber() : "")
                    .buyerLinePhone("")
                    .buyerPlaceOfBusiness("")
                    .buyerType("0")
                    .buyerCitizenship("")
                    .buyerSector("")
                    .buyerReferenceNo("")
                    .isCheckReferenceNo("0")
                    .build();
        }
        return EfrisInvoiceContent.BuyerDetails.builder()
                .buyerTin("").buyerNinBrn("").buyerPassportNum("")
                .buyerLegalName("").buyerBusinessName("").buyerAddress("")
                .buyerEmail("").buyerMobilePhone("").buyerLinePhone("")
                .buyerPlaceOfBusiness("").buyerType("0").buyerCitizenship("")
                .buyerSector("").buyerReferenceNo("").isCheckReferenceNo("0")
                .build();
    }

    // -------------------------------------------------------------------------
    // AES encryption / decryption
    // -------------------------------------------------------------------------

    /**
     * Serialise the content to JSON, then encrypt with AES-256-CBC.
     * A random 16-byte IV is prepended to the ciphertext before Base64 encoding,
     * matching the URA EFRIS VSCU specification.
     */
    private String encryptContent(EfrisInvoiceContent content) throws Exception {
        String json = objectMapper.writeValueAsString(content);
        byte[] keyBytes = efrisProperties.getPrivateKey()
                .getBytes(StandardCharsets.UTF_8);

        // URA requires a 16-byte key for AES-128 or 32-byte for AES-256
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "EFRIS private key must be 16 or 32 characters (got " + keyBytes.length + ")");
        }

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(iv));

        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));

        // Prepend IV so the receiver can decrypt
        byte[] ivAndCipher = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndCipher, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(ivAndCipher);
    }

    /**
     * Decrypt the Base64-encoded AES response from URA.
     * Assumes IV is the first 16 bytes of the decoded payload.
     */
    private EfrisInvoiceResult decryptResponse(String base64Content) throws Exception {
        byte[] keyBytes = efrisProperties.getPrivateKey()
                .getBytes(StandardCharsets.UTF_8);
        byte[] ivAndCipher = Base64.getDecoder().decode(base64Content);

        byte[] iv = new byte[16];
        byte[] ciphertext = new byte[ivAndCipher.length - 16];
        System.arraycopy(ivAndCipher, 0, iv, 0, 16);
        System.arraycopy(ivAndCipher, 16, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(iv));

        byte[] decrypted = cipher.doFinal(ciphertext);
        String json = new String(decrypted, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, EfrisInvoiceResult.class);
    }

    // -------------------------------------------------------------------------
    // API envelope builder
    // -------------------------------------------------------------------------

    private EfrisApiRequest buildApiRequest(String encryptedContent, String saleNumber) {
        String now = LocalDateTime.now().format(EFRIS_DATE_FMT);
        return EfrisApiRequest.builder()
                .data(EfrisRequestData.builder()
                        .content(encryptedContent)
                        .signature("")
                        .dataDescription(EfrisDataDescription.builder()
                                .codeType("0")
                                .encryptCode("1")
                                .zipCode("0")
                                .build())
                        .build())
                .globalInfo(EfrisGlobalInfo.builder()
                        .appId("AP04")
                        .version("1.1.20191201")
                        .dataExchangeId(saleNumber)
                        .interfaceCode("T109")
                        .requestCode("TP")
                        .requestTime(now)
                        .responseCode("BI")
                        .userName(efrisProperties.getDeviceNo())
                        .deviceMAC(efrisProperties.getDeviceMac())
                        .deviceNo(efrisProperties.getDeviceNo())
                        .tin(efrisProperties.getTin())
                        .brn("")
                        .taxpayerID("1")
                        .longitude(efrisProperties.getLongitude())
                        .latitude(efrisProperties.getLatitude())
                        .agentType("0")
                        .build())
                .returnStateInfo(new EfrisReturnStateInfo())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EfrisSubmission markSubmitted(EfrisSubmission submission,
            EfrisInvoiceResult result, String returnCode) {
        submission.setStatus(EfrisSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setUraReturnCode(returnCode);
        if (result != null) {
            if (result.getBasicInformation() != null) {
                submission.setFiscalReceiptNumber(result.getBasicInformation().getAntifakeCode());
                submission.setAntifakeCode(result.getBasicInformation().getAntifakeCode());
            }
            if (result.getSummaryDetails() != null) {
                submission.setQrCode(result.getSummaryDetails().getQrCode());
            }
        }
        submission.setErrorMessage(null);
        log.info("EFRIS invoice submitted for sale {}. FRN: {}",
                submission.getSale().getSaleNumber(), submission.getFiscalReceiptNumber());
        return submissionRepository.save(submission);
    }

    private EfrisSubmission markFailed(EfrisSubmission submission,
            String errorMessage, String returnCode) {
        submission.setStatus(EfrisSubmissionStatus.FAILED);
        submission.setErrorMessage(errorMessage);
        if (returnCode != null) {
            submission.setUraReturnCode(returnCode);
        }
        log.warn("EFRIS submission failed for sale {}: {}",
                submission.getSale().getSaleNumber(), errorMessage);
        return submissionRepository.save(submission);
    }

    private String toEfrisPaymentMode(PaymentMethod method) {
        return switch (method) {
            case CASH -> "101";
            case CARD -> "102";
            case MOBILE_MONEY -> "103";
        };
    }
}
