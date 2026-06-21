package com.artgallery.exception;

/**
 * Thrown when creating/updating an entity would violate a uniqueness rule.
 * Mapped to HTTP 409 (Conflict).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
