package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Invoice findByInvoiceNumber(String invoiceNumber);

}