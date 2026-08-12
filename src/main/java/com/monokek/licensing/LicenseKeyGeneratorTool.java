package com.monokek.licensing;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Vendor-side tool to mint a new license key — never invoked by the running
 * application (no Spring annotations, not exposed over HTTP). Run manually:
 *
 * <pre>
 * mvn -q exec:java -Dexec.mainClass=com.monokek.licensing.LicenseKeyGeneratorTool \
 *     -Dexec.args="&lt;signing-key&gt; \"Restaurant Name\" 2027-01-01"
 * </pre>
 *
 * The printed token is what a customer pastes into the app's "Activation
 * Required" screen. {@code signing-key} must be the exact same value as the
 * target deployment's {@code app.license.signing-key} / {@code LICENSE_SIGNING_KEY}.
 */
public final class LicenseKeyGeneratorTool {

    private LicenseKeyGeneratorTool() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: <signing-key> <company-name> <expiry-yyyy-MM-dd>");
            System.exit(1);
        }

        SecretKey key = Keys.hmacShaKeyFor(args[0].getBytes(StandardCharsets.UTF_8));
        String companyName = args[1];
        Date expiry = Date.from(LocalDate.parse(args[2]).atStartOfDay(ZoneId.systemDefault()).toInstant());

        String token = Jwts.builder()
                .subject(companyName)
                .issuedAt(new Date())
                .expiration(expiry)
                .signWith(key)
                .compact();

        System.out.println(token);
    }
}
