package com.aurionpro.ticketboard.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sessionCode;

    @Column(nullable = false, length = 150)
    private String device;

    @Column(nullable = false, length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    private LocalDateTime lastActive;

    private Boolean isCurrent;

    private Long userId;
}
