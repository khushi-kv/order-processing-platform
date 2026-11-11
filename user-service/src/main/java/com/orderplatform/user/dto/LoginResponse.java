package com.orderplatform.user.dto;

/** The token goes in later requests as {@code Authorization: Bearer <accessToken>}. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
