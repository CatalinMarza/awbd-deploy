package com.artgallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Art Gallery Management System.
 *
 * <p>Migrated from the original ASP.NET Core solution. The OLTP domain
 * (artworks, exhibitions, visitors, staff, loans, insurance, restorations,
 * reviews and related lookups) is exposed as a REST API backed by Spring Data
 * JPA. PostgreSQL is used for the {@code dev} profile and H2 in-memory for the
 * {@code test} profile.</p>
 */
@SpringBootApplication
public class ArtGalleryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtGalleryApplication.class, args);
    }
}
