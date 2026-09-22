package com.aurionpro.ticketboard.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String credentialType; // SSH_KEY, GPG_KEY

    @Column(nullable = false, length = 200)
    private String fingerprint;

    @Column(nullable = false, length = 30)
    private String status; // ACTIVE, REVOKED

    private Long userId;
}
