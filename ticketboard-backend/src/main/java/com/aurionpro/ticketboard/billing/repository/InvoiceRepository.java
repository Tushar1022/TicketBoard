package com.aurionpro.ticketboard.billing.repository;

import com.aurionpro.ticketboard.billing.entity.Invoice;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByProjectId(Long projectId);

    List<Invoice> findByClientId(Long clientId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%')")
    List<String> findInvoiceNumbersByPrefix(@Param("prefix") String prefix);
}