package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.dto.ProductVariantRequest;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;

public interface ProductVariantService {

    ProductVariant addVariant(ProductVariantRequest request);

    ProductVariant updateVariant(Long id, ProductVariantRequest request);

    List<ProductVariant> getVariantsByProduct(Long productId);

    ProductVariant getVariantByBarcode(String barcode);

    void deactivateVariant(Long id);

    ProductVariant adjustVariantStock(Long variantId, int quantityChange);
}
