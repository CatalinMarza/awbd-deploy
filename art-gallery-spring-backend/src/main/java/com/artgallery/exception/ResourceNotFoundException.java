package com.artgallery.exception;

/**
 * Thrown when a requested entity cannot be found. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s with id %s was not found".formatted(resourceName, id));
    }
}
