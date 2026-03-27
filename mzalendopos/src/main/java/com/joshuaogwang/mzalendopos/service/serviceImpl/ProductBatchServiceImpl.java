package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ExpiryAlertResponse;
import com.joshuaogwang.mzalendopos.dto.ProductBatchRequest;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.ProductBatch;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrder;
import com.joshuaogwang.mzalendopos.repository.ProductBatchRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.ProductVariantRepository;
import com.joshuaogwang.mzalendopos.repository.PurchaseOrderRepository;
import com.joshuaogwang.mzalendopos.service.ProductBatchService;

@Service
@Transactional
public class ProductBatchServiceImpl implements ProductBatchService {

    @Autowired
    private ProductBatchRepository batchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public ProductBatch addBatch(ProductBatchRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));

        ProductBatch batch = new ProductBatch();
        batch.setProduct(product);
        batch.setBatchNumber(request.getBatchNumber());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setQuantity(request.getQuantity());
        batch.setRemainingQuantity(request.getQuantity());
        batch.setUnitCost(request.getUnitCost());
        batch.setReceivedAt(LocalDateTime.now());

        if (request.getVariantId() != null) {
            ProductVariant variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with id: " + request.getVariantId()));
            batch.setVariant(variant);
        }

        if (request.getPurchaseOrderId() != null) {
            PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new NoSuchElementException("PurchaseOrder not found with id: " + request.getPurchaseOrderId()));
            batch.setPurchaseOrder(purchaseOrder);
        }

        return batchRepository.save(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductBatch> getBatchesByProduct(Long productId) {
        return batchRepository.findByProductIdOrderByExpiryDateAsc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiryAlertResponse> getExpiringSoon(int daysAhead) {
        LocalDate cutoff = LocalDate.now().plusDays(daysAhead);
        List<ProductBatch> batches = batchRepository.findExpiringSoon(cutoff);

        return batches.stream()
                .map(batch -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
                    return ExpiryAlertResponse.builder()
                            .batchId(batch.getId())
                            .productId(batch.getProduct().getId())
                            .productName(batch.getProduct().getName())
                            .batchNumber(batch.getBatchNumber())
                            .expiryDate(batch.getExpiryDate())
                            .daysUntilExpiry(daysUntilExpiry)
                            .remainingQuantity(batch.getRemainingQuantity())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deductBatchStock(Long productId, int quantity) {
        List<ProductBatch> batches = batchRepository.findByProductIdAndRemainingQuantityGreaterThan(productId, 0);
        // Sort by expiry date ascending (FEFO)
        batches.sort((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()));

        int totalAvailable = batches.stream().mapToInt(ProductBatch::getRemainingQuantity).sum();
        if (totalAvailable < quantity) {
            throw new IllegalStateException(
                    "Insufficient batch stock for product id " + productId +
                    ". Required: " + quantity + ", available: " + totalAvailable);
        }

        int remaining = quantity;
        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;

            int deduct = Math.min(batch.getRemainingQuantity(), remaining);
            batch.setRemainingQuantity(batch.getRemainingQuantity() - deduct);
            batchRepository.save(batch);
            remaining -= deduct;
        }
    }
}
