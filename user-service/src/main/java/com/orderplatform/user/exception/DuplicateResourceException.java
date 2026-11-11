package com.orderplatform.user.exception;

/** Thrown when a unique value (e.g. an email) is already taken; mapped to 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
