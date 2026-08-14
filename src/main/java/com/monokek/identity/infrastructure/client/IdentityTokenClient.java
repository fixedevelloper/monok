package com.monokek.identity.infrastructure.client;

import com.monokek.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Fetches and caches a client_credentials access token for this service's own
 * calls into monokek-identity (see {@link HttpUserDirectory}) — the same grant
 * type/scope registered for it by monokek-identity's ClientSeeder.
 */
@Component
class IdentityTokenClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private volatile CachedToken cached;

    IdentityTokenClient(
            @Value("${app.identity.token-uri}") String tokenUri,
            @Value("${app.identity.client-id}") String clientId,
            @Value("${app.identity.client-secret}") String clientSecret) {
        // Bounded, same reasoning as printing.infrastructure.LogoImageLoader's 3s fetch
        // timeout: accessToken() is synchronized, so an unbounded hang here would queue
        // up every other thread doing a name lookup behind it — a slow/unreachable
        // monokek-identity must degrade to a bounded delay, not a stall.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(tokenUri).requestFactory(requestFactory).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    synchronized String accessToken() {
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt.isAfter(now)) {
            return cached.value;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials&scope=internal.users.read")
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.get("access_token") == null || response.get("expires_in") == null) {
                throw ApiException.serviceUnavailable("Le service d'identité est injoignable pour le moment.");
            }
            String token = (String) response.get("access_token");
            int expiresIn = ((Number) response.get("expires_in")).intValue();
            // Refresh a bit early so a request never races an about-to-expire token.
            cached = new CachedToken(token, now.plusSeconds(Math.max(expiresIn - 30, 0)));
            return token;
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw ApiException.serviceUnavailable("Le service d'identité est injoignable pour le moment.");
        }
    }

    private record CachedToken(String value, Instant expiresAt) {
    }
}
