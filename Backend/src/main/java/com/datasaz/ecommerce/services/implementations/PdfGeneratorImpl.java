package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.entities.Invoice;
import com.datasaz.ecommerce.repositories.entities.OrderItem;
import com.datasaz.ecommerce.repositories.entities.OrderRefund;
import com.datasaz.ecommerce.services.interfaces.IPdfGenerator;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;


@Slf4j
@Service
public class PdfGeneratorImpl implements IPdfGenerator {

    @Override
    public void generatePdf(Invoice invoice) throws FileNotFoundException {
        File file = new File("invoice_" + invoice.getId() + ".pdf");
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Add invoice details
        document.add(new Paragraph("Invoice #" + invoice.getInvoiceNumber()));
        document.add(new Paragraph("Issued: " + invoice.getIssuedAt()));
        document.add(new Paragraph("Invoice for " + invoice.getBuyerInvoice().getFirstName() + " " + invoice.getBuyerInvoice().getLastName()));
        document.add(new Paragraph("Buyer: " + invoice.getOrder().getBuyer().getEmailAddress()));

        // Add order items table
        float[] columnWidths = {3, 1, 1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.addHeaderCell("Product");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Price");
        table.addHeaderCell("Total");

        for (OrderItem item : invoice.getOrder().getItems()) {
            table.addCell(item.getProductName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(String.format("$%.2f", item.getPrice()));
            table.addCell(String.format("$%.2f", item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }

        // Add totals
        BigDecimal subtotal = invoice.getOrder().getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        table.addCell("Subtotal");
        table.addCell("");
        table.addCell("");
        table.addCell(String.format("$%.2f", subtotal));

        if (invoice.getOrder().getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            table.addCell("Discount");
            table.addCell("");
            table.addCell("");
            table.addCell(String.format("-$%.2f", invoice.getOrder().getDiscountAmount()));
        }

        table.addCell("VAT");
        table.addCell("");
        table.addCell("");
        table.addCell(String.format("$%.2f", invoice.getOrder().getTotalVAT()));

        table.addCell("Total");
        table.addCell("");
        table.addCell("");
        table.addCell(String.format("$%.2f", invoice.getOrder().getTotalAmount()));

        document.add(table);
        document.close();
    }

//    @Override
//    public void generatePdf(Invoice invoice) throws FileNotFoundException {
//        // This method can be modified to generate a more detailed PDF
//        PdfWriter writer = new PdfWriter("invoice.pdf");
//        PdfDocument pdf = new PdfDocument(writer);
//        Document document = new Document(pdf);
//
//        document.add(new Paragraph("Invoice for " + invoice.getBuyerInvoice().getFirstName() + " " + invoice.getBuyerInvoice().getLastName()));
//        document.add(new Paragraph("Total Amount: $" + invoice.getTotalAmount()));
//
//        for (OrderItem item : invoice.getItems()) {
//            document.add(new Paragraph(item.getProductName() + " - Qty: " + item.getQuantity() + " - $" + item.getPrice()));
//        }
//
//        document.close();
//    }

    @Override
    public File generateRefundDocument(OrderRefund orderRefund) {
        String fileName = "refund_" + orderRefund.getId() + ".pdf";
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph(new Text("Refund Confirmation #" + orderRefund.getId())
                    .setBold().setFontSize(16)));
            document.add(new Paragraph("Refund Date: " + orderRefund.getRefundDate()));
            document.add(new Paragraph("Order ID: " + orderRefund.getPayment().getOrder().getId()));
            if (orderRefund.getReturnRequest() != null) {
                document.add(new Paragraph("Return Request ID: " + orderRefund.getReturnRequest().getId()));
            }
            document.add(new Paragraph("Reason: " + orderRefund.getReason()));

            float[] columnWidths = {200, 200};
            Table table = new Table(columnWidths);
            table.addCell("Description");
            table.addCell("Details");
            table.addCell("Refund Amount");
            table.addCell(orderRefund.getAmount().toString());
            table.addCell("Transaction ID");
            table.addCell(orderRefund.getTransactionId());
            document.add(table);

            document.close();
            log.info("Generated refund PDF: {}", fileName);
            return file;
        } catch (IOException e) {
            log.error("Failed to generate refund PDF for refund {}: {}", orderRefund.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate refund PDF: " + e.getMessage());
        }
    }
}
