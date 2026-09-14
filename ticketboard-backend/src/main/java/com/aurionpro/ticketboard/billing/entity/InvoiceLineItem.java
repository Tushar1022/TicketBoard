package com.aurionpro.ticketboard.billing.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invoice_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "work_item_number", length = 50)
    private String workItemNumber;

    @Column(name = "work_item_title", length = 350)
    private String workItemTitle;

    @Column(name = "consultant_id")
    private Long consultantId;

    @Column(name = "consultant_name", length = 150)
    private String consultantName;

    @Column(name = "billing_type", length = 50)
    private String billingType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "hours")
    private Double hours;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "amount")
    private Double amount;
}