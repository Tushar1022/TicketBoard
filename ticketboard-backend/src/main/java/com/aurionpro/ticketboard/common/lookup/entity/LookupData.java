package com.aurionpro.ticketboard.common.lookup.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lookup_data", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lookup_category_value", columnNames = {"category", "value"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "color_code", length = 20)
    private String colorCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}