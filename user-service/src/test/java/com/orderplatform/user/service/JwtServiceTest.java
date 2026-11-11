package com.orderplatform.user.service;

import com.orderplatform.user.TestUsers;
import com.orderplatform.user.config.JwtConfig;
import com.orderplatform.user.config.JwtProperties;
import com.orderplatform.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses the real encoder and decoder from JwtConfig, so it checks that what we sign can be verified. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, "order-platform", Duration.ofHours(1));
    private final SecretKey key = config.jwtSigningKey(properties);
    private final JwtService jwtService = new JwtService(config.jwtEncoder(key), properties);
    private final JwtDecoder decoder = config.jwtDecoder(key, properties);

    @Test
    void tokenCarriesTheClaimsOtherServicesRelyOn() {
        String token = jwtService.issueToken(TestUsers.user(7L, "admin@test.com", Role.ADMIN));

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("order-platform");
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@test.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(jwtService.expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties otherProperties = new JwtProperties("another-secret-that-is-at-least-32-chars", "order-platform", Duration.ofHours(1));
        SecretKey otherKey = config.jwtSigningKey(otherProperties);
        String forged = new JwtService(config.jwtEncoder(otherKey), otherProperties).issueToken(TestUsers.customer(1L));

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties otherIssuer = new JwtProperties(SECRET, "someone-else", Duration.ofHours(1));
        String token = new JwtService(config.jwtEncoder(key), otherIssuer).issueToken(TestUsers.customer(1L));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        // Issued two hours ago, expired an hour ago (well past the decoder's 60-second clock-skew allowance)
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("order-platform")
                .subject("1")
                .issuedAt(twoHoursAgo)
                .expiresAt(twoHoursAgo.plus(Duration.ofHours(1)))
                .build();
        String token = config.jwtEncoder(key)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
