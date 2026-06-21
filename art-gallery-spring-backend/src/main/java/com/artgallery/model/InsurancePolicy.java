package com.artgallery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An insurance policy covering one or more artworks.
 * Maps to the {@code insurance_policy} table.
 */
@Entity
@Table(name = "insurance_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String provider;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_coverage_amount", precision = 14, scale = 2)
    private BigDecimal totalCoverageAmount;

    /** Inverse side of {@link Insurance#getPolicy()} (demonstrates {@code @OneToMany}). */
    @OneToMany(mappedBy = "policy", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Insurance> insurances = new ArrayList<>();
}
