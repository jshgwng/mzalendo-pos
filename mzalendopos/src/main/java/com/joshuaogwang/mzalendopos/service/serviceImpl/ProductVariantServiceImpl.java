package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ProductVariantRequest;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.ProductVariantRepository;
import com.joshuaogwang.mzalendopos.service.ProductVariantService;

@Service
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public ProductVariant addVariant(ProductVariantRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(request.getVariantName());
        variant.setSku(request.getSku());
        variant.setBarcode(request.getBarcode());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setCostPrice(request.getCostPrice());
        variant.setStockLevel(request.getStockLevel());
        variant.setActive(true);

        return variantRepository.save(variant);
    }

    @Override
    public ProductVariant updateVariant(Long id, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with id: " + id));

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));
            variant.setProduct(product);
        }

        if (request.getVariantName() != null) {
            variant.setVariantName(request.getVariantName());
        }
        if (request.getSku() != null) {
            variant.setSku(request.getSku());
        }
        if (request.getBarcode() != null) {
            variant.setBarcode(request.getBarcode());
        }
        variant.setSellingPrice(request.getSellingPrice());
        variant.setCostPrice(request.getCostPrice());
        variant.setStockLevel(request.getStockLevel());

        return variantRepository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProduct(Long productId) {
        return variantRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariant getVariantByBarcode(String barcode) {
        return variantRepository.findByBarcode(barcode)
                .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with barcode: " + barcode));
    }

    @Override
    public void deactivateVariant(Long id) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with id: " + id));
        variant.setActive(false);
        variantRepository.save(variant);
    }

    @Override
    public ProductVariant adjustVariantStock(Long variantId, int quantityChange) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with id: " + variantId));

        int newStock = variant.getStockLevel() + quantityChange;
        if (newStock < 0) {
            throw new IllegalStateException(
                    "Insufficient stock for variant id " + variantId +
                    ". Current: " + variant.getStockLevel() + ", change: " + quantityChange);
        }
        variant.setStockLevel(newStock);
        return variantRepository.save(variant);
    }
}
