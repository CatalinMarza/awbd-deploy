package com.artgallery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The central domain entity: a piece of art held by the gallery.
 * Maps to the {@code artwork} table.
 */
@Entity
@Table(name = "artwork")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(name = "year_created")
    private Integer yearCreated;

    @Column(length = 64)
    private String medium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    /** Inverse side of the {@code @OneToOne} owned by {@link Acquisition#getArtwork()}. */
    @OneToOne(mappedBy = "artwork", fetch = FetchType.LAZY)
    private Acquisition acquisition;

    /** {@code @ManyToMany} association with exhibitions (owning side). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "artwork_exhibition",
            joinColumns = @JoinColumn(name = "artwork_id"),
            inverseJoinColumns = @JoinColumn(name = "exhibition_id")
    )
    @Builder.Default
    private List<Exhibition> exhibitions = new ArrayList<>();
}
