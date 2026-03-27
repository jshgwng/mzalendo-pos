package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.entity.LowStockAlert;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.repository.LowStockAlertRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.service.LowStockService;

@Service
public class LowStockServiceImpl implements LowStockService {

    @Autowired
    private LowStockAlertRepository lowStockAlertRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public List<LowStockAlert> checkAndCreateAlerts() {
        List<Product> allProducts = productRepository.findAll();

        List<LowStockAlert> unresolvedAlerts = lowStockAlertRepository.findByResolvedFalse();
        Set<Long> productsWithUnresolvedAlerts = unresolvedAlerts.stream()
                .map(alert -> alert.getProduct().getId())
                .collect(Collectors.toSet());

        List<LowStockAlert> newAlerts = new ArrayList<>();

        for (Product product : allProducts) {
            if (!product.isLowStockAlertEnabled()) {
                continue;
            }
            if (product.getStockLevel() > product.getReorderPoint()) {
                continue;
            }
            if (productsWithUnresolvedAlerts.contains(product.getId())) {
                continue;
            }

            LowStockAlert alert = new LowStockAlert();
            alert.setProduct(product);
            alert.setStockLevelAtAlert(product.getStockLevel());
            alert.setReorderPoint(product.getReorderPoint());
            alert.setAlertedAt(LocalDateTime.now());
            alert.setResolved(false);
            newAlerts.add(lowStockAlertRepository.save(alert));
        }

        return newAlerts;
    }

    @Override
    public Page<LowStockAlert> getUnresolvedAlerts(Pageable pageable) {
        return lowStockAlertRepository.findByResolvedFalse(pageable);
    }

    @Override
    public List<LowStockAlert> getAlertsByProduct(Long productId) {
        return lowStockAlertRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public LowStockAlert resolveAlert(Long alertId) {
        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Low stock alert not found with id: " + alertId));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return lowStockAlertRepository.save(alert);
    }
}
