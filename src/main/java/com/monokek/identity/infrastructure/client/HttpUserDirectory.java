package com.monokek.identity.infrastructure.client;

import com.monokek.identity.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * HTTP-backed replacement for the old in-process UserDirectory (a plain JPA query
 * against the local {@code users} table before the extraction) — calls
 * monokek-identity's {@code GET /internal/users}, authenticated with a
 * client_credentials token (see IdentityTokenClient). Short-TTL cache: other
 * modules call this on every order/session listing just to show a name, so this
 * avoids a network round-trip per request without risking a long-stale name.
 *
 * <p>A display name is a nicety, not something an order/cash-register listing
 * should ever 500 over — unlike the old local JPA query (which essentially
 * couldn't fail on its own), a slow/unreachable monokek-identity now can, so
 * every failure here is bounded (timeout) and degrades to omitting the missing
 * names rather than propagating.
 */
@Component
class HttpUserDirectory implements UserDirectory {

    private static final Logger log = LoggerFactory.getLogger(HttpUserDirectory.class);
    private static final Duration TTL = Duration.ofMinutes(2);

    private final RestClient restClient;
    private final IdentityTokenClient tokenClient;
    private final ConcurrentHashMap<Long, CachedName> cache = new ConcurrentHashMap<>();

    HttpUserDirectory(@Value("${app.identity.api-url}") String apiUrl, IdentityTokenClient tokenClient) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
        this.tokenClient = tokenClient;
    }

    @Override
    public Map<Long, String> namesByIds(Collection<Long> ids) {
        Instant now = Instant.now();
        Map<Long, String> result = new LinkedHashMap<>();
        List<Long> missing = new ArrayList<>();

        for (Long id : ids) {
            CachedName hit = cache.get(id);
            if (hit != null && hit.expiresAt.isAfter(now)) {
                result.put(id, hit.name);
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            fetch(missing).forEach((id, name) -> {
                cache.put(id, new CachedName(name, now.plus(TTL)));
                result.put(id, name);
            });
        }

        return result;
    }

    private Map<Long, String> fetch(List<Long> ids) {
        try {
            String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            UserNamesResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/users").queryParam("ids", idsParam).build())
                    .headers(headers -> headers.setBearerAuth(tokenClient.accessToken()))
                    .retrieve()
                    .body(UserNamesResponse.class);
            return response == null ? Map.of() : response.names();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les noms d'utilisateurs depuis monokek-identity: {}", e.getMessage());
            return Map.of();
        }
    }

    private record CachedName(String name, Instant expiresAt) {
    }

    private record UserNamesResponse(Map<Long, String> names) {
    }
}
