package com.orderplatform.user.repository;

import com.orderplatform.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Ends every session of the user (token reuse detected). */
    @Modifying
    @Query("delete from RefreshToken t where t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
