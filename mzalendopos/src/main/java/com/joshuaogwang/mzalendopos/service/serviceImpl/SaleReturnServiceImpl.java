package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.ReturnRequest;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.ReturnItem;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.entity.SaleReturn;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.SaleItemRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.repository.SaleReturnRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AccountingService;
import com.joshuaogwang.mzalendopos.service.SaleReturnService;

@Service
public class SaleReturnServiceImpl implements SaleReturnService {

    @Autowired
    private SaleReturnRepository returnRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountingService accountingService;

    @Override
    @Transactional
    public SaleReturn processReturn(ReturnRequest request, String cashierUsername) {
        Sale originalSale = saleRepository.findById(request.getOriginalSaleId())
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + request.getOriginalSaleId()));

        if (originalSale.getStatus() == SaleStatus.VOIDED) {
            throw new IllegalArgumentException("Cannot return items from a voided sale");
        }
        if (originalSale.getStatus() == SaleStatus.OPEN) {
            throw new IllegalArgumentException("Cannot return items from an open sale");
        }
        if (originalSale.getStatus() == SaleStatus.RETURNED) {
            throw new IllegalArgumentException("Sale has already been fully returned");
        }

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierUsername));

        List<SaleItem> saleItems = saleItemRepository.findBySaleId(originalSale.getId());
        List<ReturnItem> returnItems = new ArrayList<>();
        double totalRefund = 0.0;

        for (ReturnRequest.ReturnItemRequest ri : request.getItems()) {
            SaleItem saleItem = saleItems.stream()
                    .filter(si -> si.getProduct().getId().equals(ri.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product ID " + ri.getProductId() + " was not in the original sale"));

            if (ri.getQuantity() > saleItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Cannot return " + ri.getQuantity() + " of '" + saleItem.getProductName() +
                        "' — only " + saleItem.getQuantity() + " were sold");
            }

            double refundForItem = ri.getQuantity() * saleItem.getUnitPrice();
            totalRefund += refundForItem;

            // Restore stock
            Product product = saleItem.getProduct();
            product.setStockLevel(product.getStockLevel() + ri.getQuantity());
            productRepository.save(product);

            ReturnItem returnItem = new ReturnItem();
            returnItem.setProduct(product);
            returnItem.setProductName(saleItem.getProductName());
            returnItem.setQuantity(ri.getQuantity());
            returnItem.setUnitPrice(saleItem.getUnitPrice());
            returnItem.setRefundAmount(Math.round(refundForItem * 100.0) / 100.0);
            returnItems.add(returnItem);
        }

        // Update original sale status
        boolean allItemsReturned = request.getItems().size() == saleItems.size() &&
                request.getItems().stream().allMatch(ri ->
                    saleItems.stream().anyMatch(si ->
                        si.getProduct().getId().equals(ri.getProductId()) &&
                        ri.getQuantity().equals(si.getQuantity())));

        originalSale.setStatus(allItemsReturned ? SaleStatus.RETURNED : SaleStatus.PARTIALLY_RETURNED);
        saleRepository.save(originalSale);

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setOriginalSale(originalSale);
        saleReturn.setProcessedBy(cashier);
        saleReturn.setReason(request.getReason());
        saleReturn.setRefundAmount(Math.round(totalRefund * 100.0) / 100.0);
        saleReturn.setReturnedAt(LocalDateTime.now());
        SaleReturn saved = returnRepository.save(saleReturn);

        returnItems.forEach(ri -> ri.setSaleReturn(saved));
        saved.setItems(returnItems);
        SaleReturn finalReturn = returnRepository.save(saved);

        // Sync credit note to accounting tools (non-blocking)
        try {
            accountingService.syncReturn(finalReturn);
        } catch (Exception ex) {
            // Accounting sync failure must never roll back a processed return
        }

        return finalReturn;
    }

    @Override
    public SaleReturn getReturnById(Long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Return not found with id: " + id));
    }

    @Override
    public List<SaleReturn> getReturnsByOriginalSale(Long saleId) {
        return returnRepository.findByOriginalSaleId(saleId);
    }
}
