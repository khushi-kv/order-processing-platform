package com.orderplatform.user.service;

import com.orderplatform.user.TestUsers;
import com.orderplatform.user.config.RefreshTokenProperties;
import com.orderplatform.user.entity.RefreshToken;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.exception.InvalidRefreshTokenException;
import com.orderplatform.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;
    private final User user = TestUsers.customer(1L);

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshTokenRepository, new RefreshTokenProperties(Duration.ofDays(7)));
    }

    @Test
    void issueStoresOnlyTheHashOfARandomToken() {
        String token = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(token)).isNotEqualTo(token);
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
        assertThat(service.issue(user)).isNotEqualTo(token);
    }

    @Test
    void rotateRevokesAValidTokenAndReturnsItsUser() {
        RefreshToken stored = storedToken("valid", Instant.now().plus(Duration.ofDays(1)));

        assertThat(service.rotate("valid")).isSameAs(user);
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void reusingARevokedTokenEndsAllSessionsOfTheUser() {
        RefreshToken stored = storedToken("stolen", Instant.now().plus(Duration.ofDays(1)));
        stored.revoke(Instant.now());

        assertThatThrownBy(() -> service.rotate("stolen")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    void rotateRejectsExpiredAndUnknownTokens() {
        storedToken("expired", Instant.now().minus(Duration.ofMinutes(1)));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("unknown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("expired")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    private RefreshToken storedToken(String token, Instant expiresAt) {
        RefreshToken stored = new RefreshToken(user, RefreshTokenService.hash(token), expiresAt);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(token))).thenReturn(Optional.of(stored));
        return stored;
    }
}
