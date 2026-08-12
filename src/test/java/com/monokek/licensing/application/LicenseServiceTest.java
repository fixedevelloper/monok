package com.monokek.licensing.application;

import com.monokek.common.ApiException;
import com.monokek.licensing.domain.License;
import com.monokek.licensing.domain.LicenseRepository;
import com.monokek.licensing.web.dto.LicenseStatusDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseServiceTest {

    private static final String SIGNING_KEY = "test-signing-key-at-least-32-bytes-long!!";

    private String tokenExpiringIn(int days) {
        SecretKey key = Keys.hmacShaKeyFor(SIGNING_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("Test Restaurant")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(days, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    @Test
    void statusReportsNoLicenseWhenNoneStored() {
        LicenseRepository repository = mock(LicenseRepository.class);
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        LicenseStatusDto status = new LicenseService(repository, SIGNING_KEY).status();

        assertThat(status.active()).isFalse();
        assertThat(status.expired()).isFalse();
    }

    @Test
    void activateThenStatusReportsActiveForAFutureExpiry() {
        LicenseRepository repository = mock(LicenseRepository.class);
        String token = tokenExpiringIn(30);
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licenseWith(token)));

        LicenseService service = new LicenseService(repository, SIGNING_KEY);
        LicenseStatusDto activated = service.activate(token);
        LicenseStatusDto status = service.status();

        assertThat(activated.active()).isTrue();
        assertThat(activated.expired()).isFalse();
        assertThat(activated.daysLeft()).isBetween(29L, 30L);
        assertThat(status.active()).isTrue();
        verify(repository).save(any(License.class));
    }

    @Test
    void statusReportsExpiredForAPastExpiryWithAValidSignature() {
        LicenseRepository repository = mock(LicenseRepository.class);
        String token = tokenExpiringIn(-5);
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licenseWith(token)));

        LicenseStatusDto status = new LicenseService(repository, SIGNING_KEY).status();

        assertThat(status.active()).isFalse();
        assertThat(status.expired()).isTrue();
        assertThat(status.daysLeft()).isZero();
    }

    @Test
    void statusThrows403ForATamperedOrForeignlySignedToken() {
        LicenseRepository repository = mock(LicenseRepository.class);
        String tokenSignedWithADifferentKey = Jwts.builder()
                .subject("Forged")
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor("a-completely-different-signing-key-here!!".getBytes(StandardCharsets.UTF_8)))
                .compact();
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licenseWith(tokenSignedWithADifferentKey)));

        assertThatThrownBy(() -> new LicenseService(repository, SIGNING_KEY).status())
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(403));
    }

    @Test
    void activateRejectsAForgedKeyWithoutPersistingIt() {
        LicenseRepository repository = mock(LicenseRepository.class);
        String forged = Jwts.builder()
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor("another-wrong-signing-key-value-here!!!".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> new LicenseService(repository, SIGNING_KEY).activate(forged))
                .isInstanceOf(ApiException.class);

        verify(repository, never()).save(any(License.class));
    }

    private License licenseWith(String token) {
        License license = new License();
        license.setToken(token);
        return license;
    }
}
