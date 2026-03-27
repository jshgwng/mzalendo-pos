package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmissionStatus;
import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.entity.SaleStatus;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.CustomerRepository;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.ProductVariantRepository;
import com.joshuaogwang.mzalendopos.repository.SaleItemRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.AccountingService;
import com.joshuaogwang.mzalendopos.service.CustomerAccountService;
import com.joshuaogwang.mzalendopos.service.EfrisService;
import com.joshuaogwang.mzalendopos.service.PriceListService;
import com.joshuaogwang.mzalendopos.service.PromotionService;
import com.joshuaogwang.mzalendopos.service.SaleService;

@Service
public class SaleServiceImpl implements SaleService {

    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleItemRepository saleItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private EfrisService efrisService;
    @Autowired private AccountingService accountingService;
    @Autowired private PromotionService promotionService;
    @Autowired private PriceListService priceListService;
    @Autowired private CustomerAccountService customerAccountService;

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
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + request.getProductId()));

        // Resolve variant if provided
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NoSuchElementException("Variant not found: " + request.getVariantId()));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }
        }

        // Determine unit price: price list → variant price → product price
        double unitPrice = resolvePrice(sale, product, variant);

        final ProductVariant finalVariant = variant;
        SaleItem item;
        if (variant != null) {
            item = saleItemRepository.findBySaleIdAndProductIdAndVariantId(saleId, product.getId(), variant.getId())
                    .orElse(null);
        } else {
            item = saleItemRepository.findBySaleIdAndProductIdAndVariantIdIsNull(saleId, product.getId())
                    .orElse(null);
        }

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setLineTotal(item.getQuantity() * item.getUnitPrice());
        } else {
            item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setVariant(finalVariant);
            item.setProductName(product.getName());
            if (finalVariant != null) {
                item.setProductName(product.getName() + " - " + finalVariant.getVariantName());
            }
            item.setUnitPrice(unitPrice);
            item.setTaxRate(product.getTaxRate());
            item.setQuantity(request.getQuantity());
            item.setLineTotal(request.getQuantity() * unitPrice);
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
                .orElseThrow(() -> new NoSuchElementException("Sale item not found: " + itemId));

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
                .orElseThrow(() -> new NoSuchElementException("Sale not found: " + id));
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

        // Validate stock for each item
        for (SaleItem item : sale.getItems()) {
            int available = item.getVariant() != null
                    ? item.getVariant().getStockLevel()
                    : item.getProduct().getStockLevel();
            if (available < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for: " + item.getProductName() +
                        ". Available: " + available + ", Requested: " + item.getQuantity());
            }
        }

        // Validate total payments cover the remaining balance (after any deposit)
        double totalPaid = request.getPayments().stream()
                .mapToDouble(CheckoutRequest.PaymentSplit::getAmount)
                .sum();
        double remaining = sale.getTotalAmount() - sale.getDepositPaid();
        if (totalPaid < remaining - 0.001) {
            throw new IllegalArgumentException(
                    String.format("Amount paid (%.2f) is less than remaining balance (%.2f)",
                            totalPaid, remaining));
        }

        // Attach customer if provided
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getCustomerId()));
            sale.setCustomer(customer);
        }

        // Deduct stock
        for (SaleItem item : sale.getItems()) {
            if (item.getVariant() != null) {
                ProductVariant v = item.getVariant();
                v.setStockLevel(v.getStockLevel() - item.getQuantity());
                productVariantRepository.save(v);
            } else {
                Product p = item.getProduct();
                p.setStockLevel(p.getStockLevel() - item.getQuantity());
                productRepository.save(p);
            }
        }

        // Build payment records for each split
        double totalChange = totalPaid - remaining;
        double changeRemaining = totalChange;
        List<Payment> payments = new ArrayList<>();

        for (int i = 0; i < request.getPayments().size(); i++) {
            CheckoutRequest.PaymentSplit split = request.getPayments().get(i);
            boolean isLast = i == request.getPayments().size() - 1;

            // CREDIT: charge to customer account
            if (split.getMethod() == PaymentMethod.CREDIT) {
                if (sale.getCustomer() == null && request.getCustomerId() == null) {
                    throw new IllegalArgumentException("Customer is required for CREDIT payment");
                }
                Long customerId = sale.getCustomer() != null
                        ? sale.getCustomer().getId() : request.getCustomerId();
                customerAccountService.chargeToAccount(
                        customerId, split.getAmount(), sale.getSaleNumber(),
                        sale.getCashier().getUsername());
            }

            // Assign change only to the last CASH split
            double changeForSplit = 0.0;
            if (split.getMethod() == PaymentMethod.CASH && isLast) {
                changeForSplit = round(changeRemaining);
            } else if (split.getMethod() != PaymentMethod.CASH) {
                changeForSplit = 0.0;
            } else {
                // Non-last cash split: absorbs exact amount needed, change deferred
                double cashNeeded = Math.min(split.getAmount(), changeRemaining);
                changeRemaining = Math.max(0, changeRemaining - cashNeeded);
                changeForSplit = cashNeeded;
            }

            Payment payment = new Payment();
            payment.setSale(sale);
            payment.setMethod(split.getMethod());
            payment.setAmountPaid(split.getAmount());
            payment.setChangeGiven(changeForSplit);
            payment.setPaidAt(LocalDateTime.now());
            payments.add(paymentRepository.save(payment));
        }

        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCompletedAt(LocalDateTime.now());
        Sale completed = saleRepository.save(sale);

        // Submit fiscal invoice to URA EFRIS (non-blocking)
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

        // Sync to accounting (non-blocking)
        try {
            accountingService.syncSale(completed);
        } catch (Exception ex) {
            // Accounting sync failure must never roll back a completed sale
        }

        return completed;
    }

    @Override
    @Transactional
    public Sale holdSale(Long saleId, double depositAmount) {
        Sale sale = getOpenSale(saleId);

        if (sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot hold an empty sale");
        }
        if (depositAmount < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative");
        }

        sale.setStatus(SaleStatus.HOLD);
        sale.setDepositPaid(depositAmount);
        sale.setHeldAt(LocalDateTime.now());
        return saleRepository.save(sale);
    }

    @Override
    @Transactional
    public Sale releaseSale(Long saleId, CheckoutRequest request) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found: " + saleId));

        if (sale.getStatus() != SaleStatus.HOLD) {
            throw new IllegalArgumentException("Sale is not on hold (status: " + sale.getStatus() + ")");
        }

        // Restore to OPEN so checkout() can process it normally
        sale.setStatus(SaleStatus.OPEN);
        saleRepository.save(sale);
        return checkout(saleId, request);
    }

    @Override
    @Transactional
    public Sale voidSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found: " + saleId));

        if (sale.getStatus() == SaleStatus.VOIDED) {
            throw new IllegalArgumentException("Sale is already voided");
        }

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            for (SaleItem item : sale.getItems()) {
                if (item.getVariant() != null) {
                    ProductVariant v = item.getVariant();
                    v.setStockLevel(v.getStockLevel() + item.getQuantity());
                    productVariantRepository.save(v);
                } else {
                    Product p = item.getProduct();
                    p.setStockLevel(p.getStockLevel() + item.getQuantity());
                    productRepository.save(p);
                }
            }
        }

        sale.setStatus(SaleStatus.VOIDED);
        return saleRepository.save(sale);
    }

    @Override
    public ReceiptResponse getReceipt(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found: " + saleId));

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new IllegalArgumentException("Receipt is only available for completed sales");
        }

        List<Payment> payments = paymentRepository.findBySaleId(saleId);

        List<ReceiptResponse.ReceiptItem> receiptItems = sale.getItems().stream()
                .map(item -> ReceiptResponse.ReceiptItem.builder()
                        .productName(item.getProductName())
                        .variantName(item.getVariant() != null ? item.getVariant().getVariantName() : null)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .taxRate(item.getTaxRate())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        List<ReceiptResponse.ReceiptPayment> receiptPayments = payments.stream()
                .map(p -> ReceiptResponse.ReceiptPayment.builder()
                        .method(p.getMethod())
                        .amount(p.getAmountPaid())
                        .changeGiven(p.getChangeGiven())
                        .build())
                .collect(Collectors.toList());

        double totalAmountPaid = payments.stream().mapToDouble(Payment::getAmountPaid).sum();
        double totalChange = payments.stream().mapToDouble(Payment::getChangeGiven).sum();

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
                .promotionDiscount(sale.getPromotionDiscount())
                .payments(receiptPayments)
                .totalAmountPaid(round(totalAmountPaid))
                .changeGiven(round(totalChange))
                .fiscalReceiptNumber(sale.getFiscalReceiptNumber())
                .efrisQrCode(sale.getEfrisQrCode())
                .efrisAntifakeCode(sale.getEfrisAntifakeCode())
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Sale getOpenSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found: " + saleId));
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Sale " + sale.getSaleNumber() + " is not open (status: " + sale.getStatus() + ")");
        }
        return sale;
    }

    private double resolvePrice(Sale sale, Product product, ProductVariant variant) {
        // 1. Check price list override
        if (sale.getPriceList() != null) {
            Long variantId = variant != null ? variant.getId() : null;
            return priceListService.getPriceForProduct(sale.getPriceList().getId(), product.getId(), variantId)
                    .orElseGet(() -> variant != null ? variant.getSellingPrice() : product.getSellingPrice());
        }
        // 2. Variant price if variant is set
        if (variant != null) {
            return variant.getSellingPrice();
        }
        // 3. Default product price
        return product.getSellingPrice();
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

        // Apply active promotions
        double promotionDiscount = 0.0;
        try {
            promotionDiscount = promotionService.calculatePromotionDiscount(items, subtotal + taxAmount - discountAmount);
        } catch (Exception ex) {
            // Promotion calculation should never block a sale
        }

        sale.setSubtotal(round(subtotal));
        sale.setTaxAmount(round(taxAmount));
        sale.setDiscountAmount(round(discountAmount));
        sale.setPromotionDiscount(round(promotionDiscount));
        sale.setTotalAmount(round(subtotal + taxAmount - discountAmount - promotionDiscount));
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
