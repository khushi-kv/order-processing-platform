package com.orderplatform.user.dto;

/**
 * The access token goes in later requests as {@code Authorization: Bearer <accessToken>}; when it
 * expires, the refresh token is sent to /api/auth/refresh for a new pair.
 */
public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
