package com.aurionpro.ticketboard.erp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "erp_okr_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OKRGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String target;

    @Column(nullable = false)
    private Integer progressPercentage;

    @Column(length = 50)
    private String dueDate;

    private Long userId;
}
