package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.Customer;

public interface CustomerService {
    Page<Customer> getAllCustomers(Pageable pageable);
    Page<Customer> searchByName(String name, Pageable pageable);
    Customer getCustomerById(Long id);
    Customer getCustomerByPhone(String phoneNumber);
    Customer saveCustomer(Customer customer);
    Customer updateCustomer(Long id, Customer customer);
    void deleteCustomer(Long id);
}
