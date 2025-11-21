package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.Invoice;
import com.datasaz.ecommerce.repositories.entities.OrderRefund;

import java.io.File;
import java.io.FileNotFoundException;

public interface IPdfGenerator {

    void generatePdf(Invoice invoice) throws FileNotFoundException;

    File generateRefundDocument(OrderRefund orderRefund);
}
