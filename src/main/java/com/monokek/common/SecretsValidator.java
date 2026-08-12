package com.monokek.common;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fails the app at startup — not on first request — if {@code app.jwt.secret}
 * or {@code app.license.signing-key} are still the hardcoded placeholder
 * values from {@code application.yml}. Both ship with a default so the app
 * runs out of the box for a `mvn spring-boot:run` with no environment
 * configured at all, but that same convenience means anyone who reads the
 * (public) source knows the exact string every un-configured deployment
 * signs its JWTs and license keys with — flagged in the security audit.
 *
 * <p>Set {@code app.security.allow-default-secrets=true} ({@code
 * ALLOW_DEFAULT_SECRETS=true}) to bypass this for quick, throwaway local
 * testing — never in a real deployment.
 */
@Component
public class SecretsValidator {

    private static final String DEFAULT_JWT_SECRET = "change-me-change-me-change-me-change-me-32";
    private static final String DEFAULT_LICENSE_SIGNING_KEY = "change-me-license-signing-key-change-me-32";

    private final String jwtSecret;
    private final String licenseSigningKey;
    private final boolean allowDefaultSecrets;

    public SecretsValidator(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.license.signing-key}") String licenseSigningKey,
            @Value("${app.security.allow-default-secrets}") boolean allowDefaultSecrets) {
        this.jwtSecret = jwtSecret;
        this.licenseSigningKey = licenseSigningKey;
        this.allowDefaultSecrets = allowDefaultSecrets;
    }

    @PostConstruct
    void validate() {
        if (allowDefaultSecrets) {
            return;
        }
        if (DEFAULT_JWT_SECRET.equals(jwtSecret) && DEFAULT_LICENSE_SIGNING_KEY.equals(licenseSigningKey)) {
            throw new IllegalStateException(
                    "app.jwt.secret et app.license.signing-key sont encore aux valeurs par défaut de application.yml. "
                            + "Positionnez les variables d'environnement JWT_SECRET et LICENSE_SIGNING_KEY avant de démarrer. "
                            + "Pour un test local jetable uniquement, contournez avec ALLOW_DEFAULT_SECRETS=true.");
        }
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "app.jwt.secret est encore la valeur par défaut de application.yml. "
                            + "Positionnez la variable d'environnement JWT_SECRET avant de démarrer. "
                            + "Pour un test local jetable uniquement, contournez avec ALLOW_DEFAULT_SECRETS=true.");
        }
        if (DEFAULT_LICENSE_SIGNING_KEY.equals(licenseSigningKey)) {
            throw new IllegalStateException(
                    "app.license.signing-key est encore la valeur par défaut de application.yml. "
                            + "Positionnez la variable d'environnement LICENSE_SIGNING_KEY avant de démarrer. "
                            + "Pour un test local jetable uniquement, contournez avec ALLOW_DEFAULT_SECRETS=true.");
        }
    }
}
