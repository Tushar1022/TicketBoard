package com.aurionpro.ticketboard.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String requestCode;

    @Column(nullable = false, length = 50)
    private String leaveType; // Annual Paid, Sick Leave, Casual Leave

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 30)
    private String status; // PENDING, APPROVED, REJECTED

    private Long userId;
}
