package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.dto.ExpiryAlertResponse;
import com.joshuaogwang.mzalendopos.dto.ProductBatchRequest;
import com.joshuaogwang.mzalendopos.entity.ProductBatch;

public interface ProductBatchService {

    ProductBatch addBatch(ProductBatchRequest request);

    List<ProductBatch> getBatchesByProduct(Long productId);

    List<ExpiryAlertResponse> getExpiringSoon(int daysAhead);

    /**
     * Deduct {@code quantity} units from the product's batches using FEFO
     * (First Expired First Out). Throws IllegalStateException if there is
     * insufficient remaining quantity across all batches.
     */
    void deductBatchStock(Long productId, int quantity);
}
