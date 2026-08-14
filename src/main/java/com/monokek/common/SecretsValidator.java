package com.monokek.common;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fails the app at startup — not on first request — if {@code
 * app.license.signing-key} or {@code app.identity.client-secret} are still the
 * hardcoded placeholder values from {@code application.yml}. Both ship with a
 * default so the app runs out of the box for a `mvn spring-boot:run` with no
 * environment configured at all, but that same convenience means anyone who
 * reads the (public) source knows the exact string every un-configured
 * deployment signs its license keys with, or authenticates to monokek-identity
 * with — flagged in the security audit. {@code app.jwt.secret} used to be
 * checked here too, before authentication moved to monokek-identity — this app
 * no longer signs anything JWT-related, only validates tokens against a JWKS.
 *
 * <p>Set {@code app.security.allow-default-secrets=true} ({@code
 * ALLOW_DEFAULT_SECRETS=true}) to bypass this for quick, throwaway local
 * testing — never in a real deployment.
 */
@Component
public class SecretsValidator {

    private static final String DEFAULT_LICENSE_SIGNING_KEY = "change-me-license-signing-key-change-me-32";
    private static final String DEFAULT_IDENTITY_CLIENT_SECRET = "change-me-monokek-spring-client-secret";

    private final String licenseSigningKey;
    private final String identityClientSecret;
    private final boolean allowDefaultSecrets;

    public SecretsValidator(
            @Value("${app.license.signing-key}") String licenseSigningKey,
            @Value("${app.identity.client-secret}") String identityClientSecret,
            @Value("${app.security.allow-default-secrets}") boolean allowDefaultSecrets) {
        this.licenseSigningKey = licenseSigningKey;
        this.identityClientSecret = identityClientSecret;
        this.allowDefaultSecrets = allowDefaultSecrets;
    }

    @PostConstruct
    void validate() {
        if (allowDefaultSecrets) {
            return;
        }
        if (DEFAULT_LICENSE_SIGNING_KEY.equals(licenseSigningKey)) {
            throw new IllegalStateException(
                    "app.license.signing-key est encore la valeur par défaut de application.yml. "
                            + "Positionnez la variable d'environnement LICENSE_SIGNING_KEY avant de démarrer. "
                            + "Pour un test local jetable uniquement, contournez avec ALLOW_DEFAULT_SECRETS=true.");
        }
        if (DEFAULT_IDENTITY_CLIENT_SECRET.equals(identityClientSecret)) {
            throw new IllegalStateException(
                    "app.identity.client-secret est encore la valeur par défaut de application.yml. "
                            + "Positionnez la variable d'environnement IDENTITY_MONOKEK_SPRING_CLIENT_SECRET avant de démarrer "
                            + "(et enregistrez le même secret côté monokek-identity). "
                            + "Pour un test local jetable uniquement, contournez avec ALLOW_DEFAULT_SECRETS=true.");
        }
    }
}
