package com.aurionpro.ticketboard.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ERPAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String assetCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String category; // Hardware, Software License, Access Token

    @Column(length = 100)
    private String serialNumber;

    private LocalDate assignedDate;

    @Column(nullable = false, length = 30)
    private String status; // ACTIVE, IN_REPAIR, PENDING_RETURN

    private Long userId;
}
