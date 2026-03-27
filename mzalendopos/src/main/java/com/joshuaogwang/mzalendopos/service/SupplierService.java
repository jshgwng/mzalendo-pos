package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.dto.SupplierRequest;
import com.joshuaogwang.mzalendopos.entity.Supplier;

public interface SupplierService {
    Supplier createSupplier(SupplierRequest request);
    Supplier updateSupplier(Long id, SupplierRequest request);
    Supplier getSupplierById(Long id);
    Page<Supplier> getAllSuppliers(Pageable pageable);
    Page<Supplier> searchSuppliers(String name, Pageable pageable);
    void deactivateSupplier(Long id);
}
