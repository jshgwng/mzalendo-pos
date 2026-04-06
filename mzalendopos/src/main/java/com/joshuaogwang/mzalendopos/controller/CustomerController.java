package com.joshuaogwang.mzalendopos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.entity.Customer;
import com.joshuaogwang.mzalendopos.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Manage customer records")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    @Operation(summary = "List customers", description = "Returns a paginated list of customers, optionally filtered by name search")
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    public ResponseEntity<Page<Customer>> getAllCustomers(
            @Parameter(description = "Search by customer name") @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(customerService.searchByName(search, pageable));
        }
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/phone/{phoneNumber}")
    @Operation(summary = "Get customer by phone number")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Customer> getCustomerByPhone(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(customerService.getCustomerByPhone(phoneNumber));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    @ApiResponse(responseCode = "201", description = "Customer created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<Customer> saveCustomer(@Valid @RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    @ApiResponse(responseCode = "200", description = "Customer updated")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer")
    @ApiResponse(responseCode = "204", description = "Customer deleted")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
