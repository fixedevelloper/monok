package com.monokek.licensing.application;

import com.monokek.common.ApiException;
import com.monokek.licensing.domain.License;
import com.monokek.licensing.domain.LicenseRepository;
import com.monokek.licensing.web.dto.LicenseStatusDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Verifies license keys minted offline by {@link com.monokek.licensing.LicenseKeyGeneratorTool} —
 * a signed JWT whose own {@code exp} claim IS the license's expiry, so
 * expiration is automatic: the moment {@code now() > exp}, {@link #status()}
 * reports {@code expired=true} with no scheduled job involved. Verified
 * entirely locally against {@code app.license.signing-key} — no external
 * license server.
 */
@Service
public class LicenseService {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LicenseRepository licenseRepository;
    private final SecretKey key;

    public LicenseService(LicenseRepository licenseRepository, @Value("${app.license.signing-key}") String signingKey) {
        this.licenseRepository = licenseRepository;
        this.key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public LicenseStatusDto status() {
        Optional<License> current = licenseRepository.findTopByOrderByIdDesc();
        if (current.isEmpty()) {
            return new LicenseStatusDto(false, false, null, null, 0);
        }
        return decode(current.get().getToken());
    }

    @Transactional
    public LicenseStatusDto activate(String token) {
        LicenseStatusDto decoded = decode(token); // throws before persisting anything if the signature is bad

        License license = new License();
        license.setToken(token);
        license.setActivatedAt(LocalDateTime.now());
        licenseRepository.save(license);

        return decoded;
    }

    /**
     * A signature failure (anything but {@link ExpiredJwtException}) means the
     * stored key was hand-edited rather than issued by the key generator tool —
     * surfaced as 403 so {@code LicenseGuard} shows its tamper screen instead of
     * the plain "activation required" one.
     */
    private LicenseStatusDto decode(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return toDto(token, claims.getExpiration(), true);
        } catch (ExpiredJwtException e) {
            return toDto(token, e.getClaims().getExpiration(), false);
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Clé de licence invalide ou altérée");
        }
    }

    private LicenseStatusDto toDto(String token, Date expiration, boolean active) {
        LocalDateTime expiryDateTime = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDateTime));
        return new LicenseStatusDto(active, !active, token, expiryDateTime.format(EXPIRY_FORMAT), daysLeft);
    }
}
