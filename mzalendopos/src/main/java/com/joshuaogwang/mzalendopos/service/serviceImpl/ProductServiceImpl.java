package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> searchByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Override
    public Page<Product> getByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new NoSuchElementException("Product not found with barcode: " + barcode));
    }

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));
        existing.setName(product.getName());
        existing.setBarcode(product.getBarcode());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        existing.setVariation(product.getVariation());
        existing.setCostPrice(product.getCostPrice());
        existing.setSellingPrice(product.getSellingPrice());
        existing.setStockLevel(product.getStockLevel());
        existing.setTaxRate(product.getTaxRate());
        existing.setImageUrl(product.getImageUrl());
        return productRepository.save(existing);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NoSuchElementException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
