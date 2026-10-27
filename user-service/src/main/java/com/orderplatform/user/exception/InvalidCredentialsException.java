package com.orderplatform.user.exception;

/** Wrong email or wrong password; one message for both so callers cannot tell which. Mapped to 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
