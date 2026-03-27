package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.PurchaseOrderRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiveStockRequest;
import com.joshuaogwang.mzalendopos.entity.LowStockAlert;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrder;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrderItem;
import com.joshuaogwang.mzalendopos.entity.PurchaseOrderStatus;
import com.joshuaogwang.mzalendopos.entity.Supplier;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.LowStockAlertRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.PurchaseOrderItemRepository;
import com.joshuaogwang.mzalendopos.repository.PurchaseOrderRepository;
import com.joshuaogwang.mzalendopos.repository.SupplierRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.PurchaseOrderService;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LowStockAlertRepository lowStockAlertRepository;

    @Override
    public PurchaseOrder createOrder(PurchaseOrderRequest request, String createdByUsername) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + request.getSupplierId()));

        User createdBy = userRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + createdByUsername));

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = purchaseOrderRepository.count() + 1;
        String orderNumber = String.format("PO-%s-%04d", datePart, count);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(orderNumber);
        order.setSupplier(supplier);
        order.setCreatedBy(createdBy);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.setCreatedAt(LocalDateTime.now());
        order.setExpectedDelivery(request.getExpectedDelivery());
        order.setNotes(request.getNotes());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        double totalAmount = 0.0;
        for (PurchaseOrderRequest.PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + itemRequest.getProductId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(savedOrder);
            item.setProduct(product);
            item.setOrderedQuantity(itemRequest.getOrderedQuantity());
            item.setReceivedQuantity(0);
            item.setUnitCost(itemRequest.getUnitCost());
            double lineTotal = itemRequest.getOrderedQuantity() * itemRequest.getUnitCost();
            item.setLineTotal(lineTotal);
            purchaseOrderItemRepository.save(item);
            totalAmount += lineTotal;
        }

        savedOrder.setTotalAmount(totalAmount);
        return purchaseOrderRepository.save(savedOrder);
    }

    @Override
    public PurchaseOrder sendOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Order can only be sent when in DRAFT status. Current status: " + order.getStatus());
        }

        order.setStatus(PurchaseOrderStatus.SENT);
        order.setSentAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order);
    }

    @Override
    public PurchaseOrder receiveStock(Long id, ReceiveStockRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (order.getStatus() != PurchaseOrderStatus.SENT && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalArgumentException("Order can only receive stock when in SENT or PARTIALLY_RECEIVED status. Current status: " + order.getStatus());
        }

        for (ReceiveStockRequest.ReceiveItemRequest receiveItem : request.getItems()) {
            PurchaseOrderItem poItem = purchaseOrderItemRepository.findById(receiveItem.getPurchaseOrderItemId())
                    .orElseThrow(() -> new NoSuchElementException("Purchase order item not found with id: " + receiveItem.getPurchaseOrderItemId()));

            int remaining = poItem.getOrderedQuantity() - poItem.getReceivedQuantity();
            if (receiveItem.getReceivedQuantity() > remaining) {
                throw new IllegalArgumentException(
                        "Received quantity (" + receiveItem.getReceivedQuantity() + ") exceeds remaining quantity (" + remaining + ") for item id: " + poItem.getId());
            }

            poItem.setReceivedQuantity(poItem.getReceivedQuantity() + receiveItem.getReceivedQuantity());
            purchaseOrderItemRepository.save(poItem);

            Product product = poItem.getProduct();
            int newStockLevel = product.getStockLevel() + receiveItem.getReceivedQuantity();
            product.setStockLevel(newStockLevel);
            productRepository.save(product);

            if (newStockLevel > product.getReorderPoint()) {
                List<LowStockAlert> unresolvedAlerts = lowStockAlertRepository.findByResolvedFalse();
                for (LowStockAlert alert : unresolvedAlerts) {
                    if (alert.getProduct().getId().equals(product.getId())) {
                        alert.setResolved(true);
                        alert.setResolvedAt(LocalDateTime.now());
                        lowStockAlertRepository.save(alert);
                    }
                }
            }
        }

        List<PurchaseOrderItem> allItems = purchaseOrderItemRepository.findByPurchaseOrderId(order.getId());
        boolean allReceived = allItems.stream()
                .allMatch(item -> item.getReceivedQuantity() >= item.getOrderedQuantity());

        if (allReceived) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
            order.setReceivedAt(LocalDateTime.now());
        } else {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        return purchaseOrderRepository.save(order);
    }

    @Override
    public PurchaseOrder cancelOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT && order.getStatus() != PurchaseOrderStatus.SENT) {
            throw new IllegalArgumentException("Order can only be cancelled when in DRAFT or SENT status. Current status: " + order.getStatus());
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    @Override
    public PurchaseOrder getOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));
    }

    @Override
    public Page<PurchaseOrder> getAllOrders(PurchaseOrderStatus status, Pageable pageable) {
        if (status != null) {
            return purchaseOrderRepository.findByStatus(status, pageable);
        }
        return purchaseOrderRepository.findAll(pageable);
    }

    @Override
    public Page<PurchaseOrder> getOrdersBySupplier(Long supplierId, Pageable pageable) {
        return purchaseOrderRepository.findBySupplier_Id(supplierId, pageable);
    }
}
