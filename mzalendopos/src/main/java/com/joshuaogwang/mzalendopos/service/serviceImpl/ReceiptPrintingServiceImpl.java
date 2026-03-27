package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.entity.Payment;
import com.joshuaogwang.mzalendopos.entity.PaymentMethod;
import com.joshuaogwang.mzalendopos.entity.Sale;
import com.joshuaogwang.mzalendopos.entity.SaleItem;
import com.joshuaogwang.mzalendopos.repository.PaymentRepository;
import com.joshuaogwang.mzalendopos.repository.SaleRepository;
import com.joshuaogwang.mzalendopos.service.ReceiptPrintingService;

@Service
@Transactional(readOnly = true)
public class ReceiptPrintingServiceImpl implements ReceiptPrintingService {

    private static final int WIDTH = 48;
    private static final String DIVIDER = "-".repeat(WIDTH);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ESC/POS byte sequences
    private static final byte[] ESC_INIT       = {0x1B, 0x40};
    private static final byte[] ALIGN_CENTER   = {0x1B, 0x61, 0x01};
    private static final byte[] ALIGN_LEFT     = {0x1B, 0x61, 0x00};
    private static final byte[] BOLD_ON        = {0x1B, 0x45, 0x01};
    private static final byte[] BOLD_OFF       = {0x1B, 0x45, 0x00};
    private static final byte[] DOUBLE_HEIGHT  = {0x1B, 0x21, 0x10};
    private static final byte[] NORMAL_SIZE    = {0x1B, 0x21, 0x00};
    private static final byte[] LF             = {0x0A};
    private static final byte[] CUT            = {0x1D, 0x56, 0x41, 0x03};

    @Value("${pos.business-name:MZALENDO POS}")
    private String businessName;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // ─── Text Receipt ────────────────────────────────────────────────────────

    @Override
    public String generateTextReceipt(Long saleId) {
        Sale sale = getSale(saleId);
        List<Payment> payments = paymentRepository.findBySaleId(saleId);

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(center(businessName)).append("\n");
        sb.append(center("RECEIPT")).append("\n");
        sb.append(DIVIDER).append("\n");

        // Sale info
        sb.append("Sale #: ").append(sale.getSaleNumber()).append("\n");
        if (sale.getCompletedAt() != null) {
            sb.append("Date  : ").append(sale.getCompletedAt().format(DT_FMT)).append("\n");
        } else {
            sb.append("Date  : ").append(sale.getCreatedAt().format(DT_FMT)).append("\n");
        }
        sb.append("Cashier: ").append(sale.getCashier().getUsername()).append("\n");
        if (sale.getCustomer() != null) {
            sb.append("Customer: ").append(sale.getCustomer().getName()).append("\n");
        }
        sb.append(DIVIDER).append("\n");

        // Items
        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (item.getVariant() != null && item.getVariant().getVariantName() != null) {
                name = name + " (" + item.getVariant().getVariantName() + ")";
            }
            // Wrap long names
            if (name.length() > WIDTH) {
                name = name.substring(0, WIDTH - 3) + "...";
            }
            sb.append(name).append("\n");
            String detail = String.format("  %d x %.2f = %.2f", item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
            sb.append(detail).append("\n");
        }
        sb.append(DIVIDER).append("\n");

        // Totals
        sb.append(leftRight("Subtotal:", String.format("%.2f", sale.getSubtotal()))).append("\n");
        sb.append(leftRight("Tax:", String.format("%.2f", sale.getTaxAmount()))).append("\n");
        if (sale.getDiscountAmount() > 0) {
            sb.append(leftRight("Discount:", String.format("-%.2f", sale.getDiscountAmount()))).append("\n");
        }
        if (sale.getPromotionDiscount() > 0) {
            sb.append(leftRight("Promo Discount:", String.format("-%.2f", sale.getPromotionDiscount()))).append("\n");
        }
        sb.append(leftRight("TOTAL:", String.format("%.2f", sale.getTotalAmount()))).append("\n");
        sb.append(DIVIDER).append("\n");

        // Payments
        for (Payment payment : payments) {
            String line = leftRight(payment.getMethod().name() + ":", String.format("%.2f", payment.getAmountPaid()));
            sb.append(line).append("\n");
            if (payment.getMethod() == PaymentMethod.CASH && payment.getChangeGiven() > 0) {
                sb.append(leftRight("  Change:", String.format("%.2f", payment.getChangeGiven()))).append("\n");
            }
        }
        sb.append(DIVIDER).append("\n");

        // EFRIS footer
        if (sale.getFiscalReceiptNumber() != null && !sale.getFiscalReceiptNumber().isBlank()) {
            sb.append("EFRIS FRN: ").append(sale.getFiscalReceiptNumber()).append("\n");
        }

        sb.append(center("Thank you for shopping with us!")).append("\n");

        return sb.toString();
    }

    // ─── ESC/POS Receipt ─────────────────────────────────────────────────────

    @Override
    public byte[] generateEscPosReceipt(Long saleId) {
        Sale sale = getSale(saleId);
        List<Payment> payments = paymentRepository.findBySaleId(saleId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // Init
            out.write(ESC_INIT);

            // Business name — centered, bold, double height
            out.write(ALIGN_CENTER);
            out.write(BOLD_ON);
            out.write(DOUBLE_HEIGHT);
            writeLine(out, businessName);
            out.write(NORMAL_SIZE);
            out.write(BOLD_OFF);

            // Sub-header
            writeLine(out, "RECEIPT");
            writeLine(out, DIVIDER);

            // Sale info — left aligned
            out.write(ALIGN_LEFT);
            writeLine(out, "Sale #: " + sale.getSaleNumber());
            String dateStr = sale.getCompletedAt() != null
                    ? sale.getCompletedAt().format(DT_FMT)
                    : sale.getCreatedAt().format(DT_FMT);
            writeLine(out, "Date  : " + dateStr);
            writeLine(out, "Cashier: " + sale.getCashier().getUsername());
            if (sale.getCustomer() != null) {
                writeLine(out, "Customer: " + sale.getCustomer().getName());
            }
            writeLine(out, DIVIDER);

            // Items
            for (SaleItem item : sale.getItems()) {
                String name = item.getProductName();
                if (item.getVariant() != null && item.getVariant().getVariantName() != null) {
                    name = name + " (" + item.getVariant().getVariantName() + ")";
                }
                writeLine(out, name);
                String detail = String.format("  %d x %.2f = %.2f", item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
                writeLine(out, detail);
            }
            writeLine(out, DIVIDER);

            // Totals — bold label
            writeKeyValue(out, "Subtotal:", String.format("%.2f", sale.getSubtotal()));
            writeKeyValue(out, "Tax:", String.format("%.2f", sale.getTaxAmount()));
            if (sale.getDiscountAmount() > 0) {
                writeKeyValue(out, "Discount:", String.format("-%.2f", sale.getDiscountAmount()));
            }
            if (sale.getPromotionDiscount() > 0) {
                writeKeyValue(out, "Promo Discount:", String.format("-%.2f", sale.getPromotionDiscount()));
            }
            out.write(BOLD_ON);
            writeKeyValue(out, "TOTAL:", String.format("%.2f", sale.getTotalAmount()));
            out.write(BOLD_OFF);
            writeLine(out, DIVIDER);

            // Payments
            for (Payment payment : payments) {
                writeKeyValue(out, payment.getMethod().name() + ":", String.format("%.2f", payment.getAmountPaid()));
                if (payment.getMethod() == PaymentMethod.CASH && payment.getChangeGiven() > 0) {
                    writeKeyValue(out, "  Change:", String.format("%.2f", payment.getChangeGiven()));
                }
            }
            writeLine(out, DIVIDER);

            // EFRIS FRN
            if (sale.getFiscalReceiptNumber() != null && !sale.getFiscalReceiptNumber().isBlank()) {
                writeLine(out, "EFRIS FRN: " + sale.getFiscalReceiptNumber());
            }

            // Footer — centered
            out.write(ALIGN_CENTER);
            writeLine(out, "Thank you for shopping with us!");
            out.write(LF);
            out.write(LF);
            out.write(LF);

            // Paper cut
            out.write(CUT);

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate ESC/POS receipt", e);
        }

        return out.toByteArray();
    }

    // ─── HTML Receipt ────────────────────────────────────────────────────────

    @Override
    public String generateHtmlReceipt(Long saleId) {
        Sale sale = getSale(saleId);
        List<Payment> payments = paymentRepository.findBySaleId(saleId);

        String dateStr = sale.getCompletedAt() != null
                ? sale.getCompletedAt().format(DT_FMT)
                : sale.getCreatedAt().format(DT_FMT);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>Receipt - ").append(escapeHtml(sale.getSaleNumber())).append("</title>");
        html.append("<style>");
        html.append("body { font-family: 'Courier New', monospace; font-size: 12px; max-width: 380px; margin: 0 auto; padding: 10px; }");
        html.append("h1 { text-align: center; font-size: 16px; margin: 4px 0; }");
        html.append("h2 { text-align: center; font-size: 13px; margin: 2px 0; font-weight: normal; }");
        html.append(".divider { border-top: 1px dashed #000; margin: 6px 0; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th { text-align: left; border-bottom: 1px solid #000; padding: 2px 4px; }");
        html.append("td { padding: 2px 4px; }");
        html.append(".right { text-align: right; }");
        html.append(".total-row td { font-weight: bold; border-top: 1px solid #000; }");
        html.append(".footer { text-align: center; margin-top: 10px; font-style: italic; }");
        html.append(".info-row { display: flex; justify-content: space-between; }");
        html.append("@media print { body { max-width: 100%; } }");
        html.append("</style></head><body>");

        // Header
        html.append("<h1>").append(escapeHtml(businessName)).append("</h1>");
        html.append("<h2>RECEIPT</h2>");
        html.append("<div class='divider'></div>");

        // Sale info
        html.append("<div class='info-row'><span>Sale #:</span><span>").append(escapeHtml(sale.getSaleNumber())).append("</span></div>");
        html.append("<div class='info-row'><span>Date:</span><span>").append(escapeHtml(dateStr)).append("</span></div>");
        html.append("<div class='info-row'><span>Cashier:</span><span>").append(escapeHtml(sale.getCashier().getUsername())).append("</span></div>");
        if (sale.getCustomer() != null) {
            html.append("<div class='info-row'><span>Customer:</span><span>").append(escapeHtml(sale.getCustomer().getName())).append("</span></div>");
        }
        html.append("<div class='divider'></div>");

        // Items table
        html.append("<table>");
        html.append("<thead><tr><th>Item</th><th class='right'>Qty</th><th class='right'>Price</th><th class='right'>Total</th></tr></thead>");
        html.append("<tbody>");
        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (item.getVariant() != null && item.getVariant().getVariantName() != null) {
                name = name + " (" + item.getVariant().getVariantName() + ")";
            }
            html.append("<tr>");
            html.append("<td>").append(escapeHtml(name)).append("</td>");
            html.append("<td class='right'>").append(item.getQuantity()).append("</td>");
            html.append("<td class='right'>").append(String.format("%.2f", item.getUnitPrice())).append("</td>");
            html.append("<td class='right'>").append(String.format("%.2f", item.getLineTotal())).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<div class='divider'></div>");

        // Totals
        html.append("<table>");
        html.append("<tr><td>Subtotal</td><td class='right'>").append(String.format("%.2f", sale.getSubtotal())).append("</td></tr>");
        html.append("<tr><td>Tax</td><td class='right'>").append(String.format("%.2f", sale.getTaxAmount())).append("</td></tr>");
        if (sale.getDiscountAmount() > 0) {
            html.append("<tr><td>Discount</td><td class='right'>-").append(String.format("%.2f", sale.getDiscountAmount())).append("</td></tr>");
        }
        if (sale.getPromotionDiscount() > 0) {
            html.append("<tr><td>Promo Discount</td><td class='right'>-").append(String.format("%.2f", sale.getPromotionDiscount())).append("</td></tr>");
        }
        html.append("<tr class='total-row'><td>TOTAL</td><td class='right'>").append(String.format("%.2f", sale.getTotalAmount())).append("</td></tr>");
        html.append("</table>");
        html.append("<div class='divider'></div>");

        // Payments
        html.append("<table>");
        for (Payment payment : payments) {
            html.append("<tr><td>").append(escapeHtml(payment.getMethod().name())).append("</td>");
            html.append("<td class='right'>").append(String.format("%.2f", payment.getAmountPaid())).append("</td></tr>");
            if (payment.getMethod() == PaymentMethod.CASH && payment.getChangeGiven() > 0) {
                html.append("<tr><td>&nbsp;&nbsp;Change</td><td class='right'>").append(String.format("%.2f", payment.getChangeGiven())).append("</td></tr>");
            }
        }
        html.append("</table>");

        // EFRIS section
        if (sale.getFiscalReceiptNumber() != null && !sale.getFiscalReceiptNumber().isBlank()) {
            html.append("<div class='divider'></div>");
            html.append("<div style='font-size:10px;'>");
            html.append("<div><strong>EFRIS FRN:</strong> ").append(escapeHtml(sale.getFiscalReceiptNumber())).append("</div>");
            if (sale.getEfrisAntifakeCode() != null && !sale.getEfrisAntifakeCode().isBlank()) {
                html.append("<div><strong>Antifake Code:</strong> ").append(escapeHtml(sale.getEfrisAntifakeCode())).append("</div>");
            }
            if (sale.getEfrisQrCode() != null && !sale.getEfrisQrCode().isBlank()) {
                html.append("<div style='word-break:break-all;'><strong>QR:</strong> ").append(escapeHtml(sale.getEfrisQrCode())).append("</div>");
            }
            html.append("</div>");
        }

        html.append("<div class='divider'></div>");
        html.append("<div class='footer'>Thank you for shopping with us!</div>");
        html.append("</body></html>");

        return html.toString();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Sale getSale(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("Sale not found with id: " + saleId));
    }

    private String center(String text) {
        if (text.length() >= WIDTH) return text;
        int padding = (WIDTH - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String leftRight(String left, String right) {
        int spacesNeeded = WIDTH - left.length() - right.length();
        if (spacesNeeded < 1) spacesNeeded = 1;
        return left + " ".repeat(spacesNeeded) + right;
    }

    private void writeLine(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes("UTF-8"));
        out.write(LF);
    }

    private void writeKeyValue(ByteArrayOutputStream out, String key, String value) throws IOException {
        writeLine(out, leftRight(key, value));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
