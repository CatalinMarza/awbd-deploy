package com.artgallery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The acquisition record of an artwork (how it entered the gallery).
 * Each artwork is acquired at most once, so this is a {@code @OneToOne}
 * relationship enforced by a unique foreign key. Maps to the {@code acquisition} table.
 */
@Entity
@Table(name = "acquisition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acquisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning side of the {@code @OneToOne} with {@link Artwork} (unique FK). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artwork_id", nullable = false, unique = true)
    private Artwork artwork;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_type", nullable = false, length = 32)
    private String acquisitionType;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;
}
