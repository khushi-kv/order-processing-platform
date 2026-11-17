package com.orderplatform.user.exception;

/** Unknown, expired, revoked or reused refresh token; one message for all of them. Mapped to 401. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
