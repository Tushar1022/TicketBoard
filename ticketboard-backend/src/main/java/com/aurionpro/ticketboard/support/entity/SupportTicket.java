package com.aurionpro.ticketboard.support.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_supportticket_status", columnList = "status"),
    @Index(name = "idx_supportticket_priority", columnList = "priority"),
    @Index(name = "idx_supportticket_category", columnList = "category"),
    @Index(name = "idx_supportticket_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", unique = true, nullable = false, length = 50)
    private String ticketCode;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private SupportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority;

    @Column(name = "target_role", nullable = false, length = 50)
    private String targetRole; // e.g. ROLE_SUPER_ADMIN, ROLE_ADMIN

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_name", nullable = false, length = 150)
    private String createdByName;

    @Column(name = "created_by_email", nullable = false, length = 150)
    private String createdByEmail;

    @Column(name = "assigned_to_id")
    private Long assignedToId;

    @Column(name = "assigned_to_name", length = 150)
    private String assignedToName;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "system_diagnostics", length = 500)
    private String systemDiagnostics;

    @Column(name = "custom_category_name", length = 100)
    private String customCategoryName;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name", length = 150)
    private String projectName;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "support_ticket_attachments", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "file_url")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TicketComment> comments = new ArrayList<>();
}
