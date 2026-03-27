package com.joshuaogwang.mzalendopos.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.CreditPaymentRequest;
import com.joshuaogwang.mzalendopos.dto.CustomerAccountRequest;
import com.joshuaogwang.mzalendopos.entity.CreditTransaction;
import com.joshuaogwang.mzalendopos.entity.CustomerAccount;
import com.joshuaogwang.mzalendopos.service.CustomerAccountService;

@RestController
@RequestMapping("/api/v1/customer-accounts")
public class CustomerAccountController {

    @Autowired
    private CustomerAccountService customerAccountService;

    @PostMapping
    public ResponseEntity<CustomerAccount> openAccount(@Valid @RequestBody CustomerAccountRequest request) {
        return ResponseEntity.ok(customerAccountService.openAccount(request));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerAccount>> getAllAccounts(Pageable pageable) {
        return ResponseEntity.ok(customerAccountService.getAllAccounts(pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CustomerAccount> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerAccountService.getAccountByCustomer(customerId));
    }

    @PostMapping("/{accountId}/payment")
    public ResponseEntity<CreditTransaction> recordPayment(
            @PathVariable Long accountId,
            @Valid @RequestBody CreditPaymentRequest request) {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return ResponseEntity.ok(customerAccountService.recordPayment(accountId, request, username));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<CreditTransaction>> getTransactions(@PathVariable Long accountId, Pageable pageable) {
        return ResponseEntity.ok(customerAccountService.getTransactions(accountId, pageable));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long accountId) {
        customerAccountService.deactivateAccount(accountId);
        return ResponseEntity.noContent().build();
    }
}
