package com.orderplatform.user.exception;

/** Thrown when a requested record does not exist; mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
