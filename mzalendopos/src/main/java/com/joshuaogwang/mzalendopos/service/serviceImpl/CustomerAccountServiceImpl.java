package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.CreditPaymentRequest;
import com.joshuaogwang.mzalendopos.dto.CustomerAccountRequest;
import com.joshuaogwang.mzalendopos.entity.CreditTransaction;
import com.joshuaogwang.mzalendopos.entity.CreditTransactionType;
import com.joshuaogwang.mzalendopos.entity.Customer;
import com.joshuaogwang.mzalendopos.entity.CustomerAccount;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.CreditTransactionRepository;
import com.joshuaogwang.mzalendopos.repository.CustomerAccountRepository;
import com.joshuaogwang.mzalendopos.repository.CustomerRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.CustomerAccountService;

@Service
@Transactional
public class CustomerAccountServiceImpl implements CustomerAccountService {

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CustomerAccount openAccount(CustomerAccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found with id: " + request.getCustomerId()));

        customerAccountRepository.findByCustomerId(request.getCustomerId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Account already exists for customer id: " + request.getCustomerId());
                });

        CustomerAccount account = new CustomerAccount();
        account.setCustomer(customer);
        account.setCreditLimit(request.getCreditLimit());
        account.setOutstandingBalance(0.0);
        account.setActive(true);
        account.setCreatedAt(LocalDateTime.now());
        return customerAccountRepository.save(account);
    }

    @Override
    public CustomerAccount getAccountByCustomer(Long customerId) {
        return customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("No account found for customer id: " + customerId));
    }

    @Override
    public Page<CustomerAccount> getAllAccounts(Pageable pageable) {
        return customerAccountRepository.findByActiveTrue(pageable);
    }

    @Override
    public CreditTransaction recordPayment(Long accountId, CreditPaymentRequest request, String processedByUsername) {
        CustomerAccount account = customerAccountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Customer account not found with id: " + accountId));

        User processedBy = userRepository.findByUsername(processedByUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + processedByUsername));

        double newBalance = Math.max(0.0, account.getOutstandingBalance() - request.getAmount());

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAccount(account);
        transaction.setType(CreditTransactionType.PAYMENT);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(newBalance);
        transaction.setProcessedBy(processedBy);
        transaction.setTransactionAt(LocalDateTime.now());
        transaction.setNotes(request.getNotes());

        account.setOutstandingBalance(newBalance);
        account.setLastTransactionAt(LocalDateTime.now());
        customerAccountRepository.save(account);

        return creditTransactionRepository.save(transaction);
    }

    @Override
    public CreditTransaction chargeToAccount(Long customerId, double amount, String saleReference, String processedByUsername) {
        CustomerAccount account = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("No account found for customer id: " + customerId));

        if (!account.isActive()) {
            throw new IllegalArgumentException("Customer account is not active for customer id: " + customerId);
        }

        if (account.getOutstandingBalance() + amount > account.getCreditLimit()) {
            throw new IllegalArgumentException("Charge would exceed credit limit. Outstanding: " + account.getOutstandingBalance()
                    + ", Charge: " + amount + ", Limit: " + account.getCreditLimit());
        }

        User processedBy = userRepository.findByUsername(processedByUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + processedByUsername));

        double newBalance = account.getOutstandingBalance() + amount;

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAccount(account);
        transaction.setType(CreditTransactionType.CHARGE);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setSaleReference(saleReference);
        transaction.setProcessedBy(processedBy);
        transaction.setTransactionAt(LocalDateTime.now());

        account.setOutstandingBalance(newBalance);
        account.setLastTransactionAt(LocalDateTime.now());
        customerAccountRepository.save(account);

        return creditTransactionRepository.save(transaction);
    }

    @Override
    public Page<CreditTransaction> getTransactions(Long accountId, Pageable pageable) {
        return creditTransactionRepository.findByAccountId(accountId, pageable);
    }

    @Override
    public void deactivateAccount(Long accountId) {
        CustomerAccount account = customerAccountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Customer account not found with id: " + accountId));
        account.setActive(false);
        customerAccountRepository.save(account);
    }
}
