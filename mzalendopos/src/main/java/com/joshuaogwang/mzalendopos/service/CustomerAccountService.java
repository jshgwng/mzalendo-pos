package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.CreditPaymentRequest;
import com.joshuaogwang.mzalendopos.dto.CustomerAccountRequest;
import com.joshuaogwang.mzalendopos.entity.CreditTransaction;
import com.joshuaogwang.mzalendopos.entity.CustomerAccount;

public interface CustomerAccountService {
    CustomerAccount openAccount(CustomerAccountRequest request);
    CustomerAccount getAccountByCustomer(Long customerId);
    Page<CustomerAccount> getAllAccounts(Pageable pageable);
    CreditTransaction recordPayment(Long accountId, CreditPaymentRequest request, String processedByUsername);
    CreditTransaction chargeToAccount(Long customerId, double amount, String saleReference, String processedByUsername);
    Page<CreditTransaction> getTransactions(Long accountId, Pageable pageable);
    void deactivateAccount(Long accountId);
}
