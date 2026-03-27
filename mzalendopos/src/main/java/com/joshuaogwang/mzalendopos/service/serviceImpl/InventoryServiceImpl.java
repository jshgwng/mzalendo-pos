package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.StockAdjustmentRequest;
import com.joshuaogwang.mzalendopos.entity.AdjustmentType;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.StockAdjustment;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.StockAdjustmentRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AccountingService;
import com.joshuaogwang.mzalendopos.service.InventoryService;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private StockAdjustmentRepository adjustmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountingService accountingService;

    @Override
    @Transactional
    public StockAdjustment adjustStock(StockAdjustmentRequest request, String username) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // Reductions (damage, theft) use a negative effective quantity
        boolean isReduction = request.getAdjustmentType() == AdjustmentType.DAMAGE ||
                              request.getAdjustmentType() == AdjustmentType.THEFT;
        int effectiveQty = isReduction ? -Math.abs(request.getQuantity()) : Math.abs(request.getQuantity());

        int previousStock = product.getStockLevel();
        int newStock = previousStock + effectiveQty;

        if (newStock < 0) {
            throw new IllegalArgumentException(
                    "Adjustment would result in negative stock. Current: " + previousStock +
                    ", Adjustment: " + effectiveQty);
        }

        product.setStockLevel(newStock);
        productRepository.save(product);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(product);
        adjustment.setAdjustmentType(request.getAdjustmentType());
        adjustment.setQuantity(effectiveQty);
        adjustment.setPreviousStockLevel(previousStock);
        adjustment.setNewStockLevel(newStock);
        adjustment.setReason(request.getReason());
        adjustment.setAdjustedBy(user);
        adjustment.setAdjustedAt(LocalDateTime.now());
        StockAdjustment saved = adjustmentRepository.save(adjustment);

        // Sync inventory adjustment to accounting tools (non-blocking)
        try {
            accountingService.syncStockAdjustment(saved);
        } catch (Exception ex) {
            // Accounting sync failure must never roll back a stock adjustment
        }

        return saved;
    }

    @Override
    public Page<StockAdjustment> getAllAdjustments(Pageable pageable) {
        return adjustmentRepository.findAll(pageable);
    }

    @Override
    public Page<StockAdjustment> getAdjustmentsByProduct(Long productId, Pageable pageable) {
        return adjustmentRepository.findByProductId(productId, pageable);
    }
}
