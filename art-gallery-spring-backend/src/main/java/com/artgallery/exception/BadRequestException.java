package com.artgallery.exception;

/**
 * Thrown when a request violates a business rule that is not a simple field
 * validation (e.g. end date before start date). Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
