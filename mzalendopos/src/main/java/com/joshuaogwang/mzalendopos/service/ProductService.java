package com.joshuaogwang.mzalendopos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.joshuaogwang.mzalendopos.entity.Product;

public interface ProductService {
    Page<Product> getAllProducts(Pageable pageable);
    Page<Product> searchByName(String name, Pageable pageable);
    Page<Product> getByCategory(Long categoryId, Pageable pageable);
    Product getProductById(Long id);
    Product getProductByBarcode(String barcode);
    Product saveProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
}
