package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.CheckoutRequest;
import com.joshuaogwang.mzalendopos.dto.DiscountRequest;
import com.joshuaogwang.mzalendopos.dto.ReceiptResponse;
import com.joshuaogwang.mzalendopos.dto.SaleItemRequest;
import com.joshuaogwang.mzalendopos.entity.Customer;
import com.joshuaogwang.mzalendopos.entity.DiscountType;
import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmissionStatus;
import com.joshuaogwang.mzalendopos.repository.CustomerRepository;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.SaleItemRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AccountingService;
import com.joshuaogwang.mzalendopos.service.EfrisService;
import com.joshuaogwang.mzalendopos.service.SaleService;

@Service
public class SaleServiceImpl implements SaleService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EfrisService efrisService;

    @Autowired
    private AccountingService accountingService;

    @Override
    @Transactional
    public Sale openSale(String cashierUsername) {
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new NoSuchElementException("Cashier not found: " + cashierUsername));

        Sale sale = new Sale();
        sale.setCashier(cashier);
        sale.setStatus(SaleStatus.OPEN);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setSaleNumber(generateSaleNumber());
        return saleRepository.save(sale);
    }

    @Override
    @Transactional
    public SaleItem addItem(Long saleId, SaleItemRequest request) {
        Sale sale = getOpenSale(saleId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));

        SaleItem item = saleItemRepository.findBySaleIdAndProductId(saleId, product.getId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setLineTotal(item.getQuantity() * item.getUnitPrice());
        } else {
            item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getSellingPrice());
            item.setTaxRate(product.getTaxRate());
            item.setQuantity(request.getQuantity());
            item.setLineTotal(request.getQuantity() * product.getSellingPrice());
        }

        SaleItem saved = saleItemRepository.save(item);
        recalculateSaleTotals(sale);
        return saved;
    }

    @Override
    @Transactional
    public SaleItem updateItemQuantity(Long saleId, Long itemId, int quantity) {
        getOpenSale(saleId);

        SaleItem item = saleItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Sale item not found with id: " + itemId));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        item.setQuantity(quantity);
        item.setLineTotal(quantity * item.getUnitPrice());
        SaleItem saved = saleItemRepository.save(item);

        Sale sale = saleRepository.findById(saleId).orElseThrow();
        recalculateSaleTotals(sale);
        return saved;
    }

    @Override
    @Transactional
    public void removeItem(Long saleId, Long itemId) {
        getOpenSale(saleId);
        saleItemRepository.deleteById(itemId);
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        recalculateSaleTotals(sale);
    }

    @Override
    @Transactional
    public Sale applyDiscount(Long saleId, DiscountRequest request) {
        Sale sale = getOpenSale(saleId);

        if (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue() > 100) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }

        sale.setDiscountType(request.getDiscountType());
        sale.setDiscountValue(request.getDiscountValue());
        recalculateSaleTotals(sale);
        return saleRepository.findById(saleId).orElseThrow();
    }

    @Override
    @Transactional
    public Sale removeDiscount(Long saleId) {
        Sale sale = getOpenSale(saleId);
        sale.setDiscountType(null);
        sale.setDiscountValue(0.0);
        sale.setDiscountAmount(0.0);
        recalculateSaleTotals(sale);
        return saleRepository.findById(saleId).orElseThrow();
    }

    @Override
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + id));
    }

    @Override
    public Page<Sale> getAllSales(SaleStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (status != null && from != null && to != null) {
            return saleRepository.findByStatusAndCreatedAtBetween(status, from, to, pageable);
        }
        if (status != null) {
            return saleRepository.findByStatus(status, pageable);
        }
        if (from != null && to != null) {
            return saleRepository.findByCreatedAtBetween(from, to, pageable);
        }
        return saleRepository.findAll(pageable);
    }

    @Override
    public Page<Sale> getSalesByCashier(Long cashierId, Pageable pageable) {
        return saleRepository.findByCashierId(cashierId, pageable);
    }

    @Override
    @Transactional
    public Sale checkout(Long saleId, CheckoutRequest request) {
        Sale sale = getOpenSale(saleId);

        if (sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout an empty sale");
        }

        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            if (product.getStockLevel() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockLevel() +
                        ", Requested: " + item.getQuantity());
            }
        }

        if (request.getAmountPaid() < sale.getTotalAmount()) {
            throw new IllegalArgumentException(
                    String.format("Amount paid (%.2f) is less than total (%.2f)",
                            request.getAmountPaid(), sale.getTotalAmount()));
        }

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NoSuchElementException("Customer not found with id: " + request.getCustomerId()));
            sale.setCustomer(customer);
        }

        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            product.setStockLevel(product.getStockLevel() - item.getQuantity());
            productRepository.save(product);
        }

        double change = request.getAmountPaid() - sale.getTotalAmount();
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setMethod(request.getPaymentMethod());
        payment.setAmountPaid(request.getAmountPaid());
        payment.setChangeGiven(Math.round(change * 100.0) / 100.0);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCompletedAt(LocalDateTime.now());
        Sale completed = saleRepository.save(sale);

        // Submit fiscal invoice to URA EFRIS (non-blocking on failure)
        try {
            EfrisSubmission submission = efrisService.submitInvoice(completed);
            if (submission.getStatus() == EfrisSubmissionStatus.SUBMITTED) {
                completed.setFiscalReceiptNumber(submission.getFiscalReceiptNumber());
                completed.setEfrisQrCode(submission.getQrCode());
                completed.setEfrisAntifakeCode(submission.getAntifakeCode());
                completed = saleRepository.save(completed);
            }
        } catch (Exception ex) {
            // EFRIS failure must never roll back a completed payment
        }

        // Sync to accounting tools (non-blocking on failure)
        try {
            accountingService.syncSale(completed);
        } catch (Exception ex) {
            // Accounting sync failure must never roll back a completed sale
        }

        return completed;
    }

    @Override
    @Transactional
    public Sale voidSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + saleId));

        if (sale.getStatus() == SaleStatus.VOIDED) {
            throw new IllegalArgumentException("Sale is already voided");
        }

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                product.setStockLevel(product.getStockLevel() + item.getQuantity());
                productRepository.save(product);
            }
        }

        sale.setStatus(SaleStatus.VOIDED);
        return saleRepository.save(sale);
    }

    @Override
    public ReceiptResponse getReceipt(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + saleId));

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new IllegalArgumentException("Receipt is only available for completed sales");
        }

        Payment payment = paymentRepository.findBySaleId(saleId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for sale: " + saleId));

        List<ReceiptResponse.ReceiptItem> receiptItems = sale.getItems().stream()
                .map(item -> ReceiptResponse.ReceiptItem.builder()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .taxRate(item.getTaxRate())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        String cashierName = sale.getCashier().getFirstName() + " " + sale.getCashier().getLastName();
        String customerName = sale.getCustomer() != null ? sale.getCustomer().getName() : null;

        return ReceiptResponse.builder()
                .saleNumber(sale.getSaleNumber())
                .cashier(cashierName)
                .customer(customerName)
                .completedAt(sale.getCompletedAt())
                .items(receiptItems)
                .subtotal(sale.getSubtotal())
                .taxAmount(sale.getTaxAmount())
                .discountType(sale.getDiscountType())
                .discountValue(sale.getDiscountValue())
                .discountAmount(sale.getDiscountAmount())
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(payment.getMethod())
                .amountPaid(payment.getAmountPaid())
                .changeGiven(payment.getChangeGiven())
                .fiscalReceiptNumber(sale.getFiscalReceiptNumber())
                .efrisQrCode(sale.getEfrisQrCode())
                .efrisAntifakeCode(sale.getEfrisAntifakeCode())
                .build();
    }

    private Sale getOpenSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + saleId));
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new IllegalArgumentException("Sale " + sale.getSaleNumber() + " is not open (status: " + sale.getStatus() + ")");
        }
        return sale;
    }

    private void recalculateSaleTotals(Sale sale) {
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());

        double subtotal = items.stream().mapToDouble(SaleItem::getLineTotal).sum();
        double taxAmount = items.stream()
                .mapToDouble(item -> item.getLineTotal() * (item.getTaxRate() / 100.0))
                .sum();

        double discountAmount = 0.0;
        if (sale.getDiscountType() != null && sale.getDiscountValue() > 0) {
            double base = subtotal + taxAmount;
            discountAmount = sale.getDiscountType() == DiscountType.PERCENTAGE
                    ? base * (sale.getDiscountValue() / 100.0)
                    : Math.min(sale.getDiscountValue(), base);
        }

        sale.setSubtotal(round(subtotal));
        sale.setTaxAmount(round(taxAmount));
        sale.setDiscountAmount(round(discountAmount));
        sale.setTotalAmount(round(subtotal + taxAmount - discountAmount));
        saleRepository.save(sale);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String generateSaleNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = saleRepository.count() + 1;
        return String.format("SALE-%s-%04d", datePart, count);
    }
}
