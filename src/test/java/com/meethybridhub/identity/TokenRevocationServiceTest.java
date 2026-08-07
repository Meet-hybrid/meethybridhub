package com.meethybridhub.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenRevocationService} (no Spring context):
 * hashing, idempotent revocation, unparseable-token handling, and the
 * concurrent-revoke guard.
 */
@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.valid.refresh.token";

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private JwtService jwtService;

    @Test
    void revokeStoresHashNotTheToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(jwtService.extractExpiration(TOKEN)).thenReturn(expiresAt);
        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);

        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        service.revoke(TOKEN, 42L);

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());

        RevokedToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(sha256(TOKEN)); // hash, never the raw token
        assertThat(saved.getTokenHash()).doesNotContain(TOKEN);
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void revokeIsIdempotent() {
        when(jwtService.extractExpiration(TOKEN)).thenReturn(Instant.now().plusSeconds(3600));
        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        service.revoke(TOKEN, 42L);

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revokeIgnoresUnparseableToken() {
        when(jwtService.extractExpiration(TOKEN)).thenThrow(new RuntimeException("bad token"));

        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        service.revoke(TOKEN, 42L);

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revokeIgnoresBlankToken() {
        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        service.revoke("   ", 42L);
        service.revoke(null, 42L);

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void concurrentRevokeIsSwallowed() {
        when(jwtService.extractExpiration(TOKEN)).thenReturn(Instant.now().plusSeconds(3600));
        when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(revokedTokenRepository.save(any(RevokedToken.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        service.revoke(TOKEN, 42L); // must not throw

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void isRevokedChecksTheHash() {
        when(revokedTokenRepository.existsByTokenHash(sha256(TOKEN))).thenReturn(true);

        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        assertThat(service.isRevoked(TOKEN)).isTrue();

        verify(revokedTokenRepository).existsByTokenHash(sha256(TOKEN));
    }

    @Test
    void isRevokedFalseForBlankToken() {
        TokenRevocationService service = new TokenRevocationService(revokedTokenRepository, jwtService);
        assertThat(service.isRevoked(null)).isFalse();
        assertThat(service.isRevoked("")).isFalse();
    }

    /** Mirror of the service's SHA-256 hex helper (test-side oracle). */
    private String sha256(String token) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
