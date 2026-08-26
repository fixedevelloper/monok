package com.monokek.identity.infrastructure.client;

import com.monokek.common.ApiException;
import com.monokek.identity.ManagerAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Calls monokek-identity's {@code POST /api/auth/lookup-pin} with the caller's own bearer token
 * (same forwarding pattern as {@code pms.internal.PmsClientService}) — that endpoint only requires
 * an authenticated caller, not a specific role, since identifying a PIN owner is the whole point.
 * The role check that actually gates the override happens here, not there.
 */
@Component
class HttpManagerAuthClient implements ManagerAuthClient {

    // roles.name is stored lowercase in the DB (see V2__seed_rbac.sql) and UserSummary.role()
    // passes it through verbatim (unlike the "ROLE_ADMIN"-style authorities Spring Security
    // builds for @PreAuthorize) — compare case-insensitively so this doesn't silently reject
    // every real manager the moment casing differs from what's assumed here.
    private static final Set<String> APPROVER_ROLES = Set.of("ADMIN", "MANAGER");

    private final RestClient restClient;

    HttpManagerAuthClient(@Value("${app.identity.api-url}") String apiUrl) {
        // Same reasoning/bound as the sibling identity clients in this package (HttpUserDirectory,
        // IdentityTokenClient): a slow/unreachable monokek-identity must fail the manager-PIN
        // check within a few seconds, not hang the cashier's cancel/void request indefinitely —
        // the default request factory has no timeout at all.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
    }

    @Override
    public ManagerApproval verifyManagerPin(Long branchId, String pin, String bearerToken) {
        LookupPinEnvelope envelope = call(() -> restClient.post()
                .uri("/api/auth/lookup-pin")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LookupPinRequestBody(branchId, pin))
                .retrieve()
                .body(LookupPinEnvelope.class));

        UserSummary user = envelope == null ? null : envelope.data();
        if (user == null) {
            throw ApiException.unauthorized("Code PIN invalide.");
        }
        if (user.role() == null || !APPROVER_ROLES.contains(user.role().toUpperCase())) {
            throw ApiException.forbidden("Ce code PIN n'appartient pas à un manager.");
        }
        return new ManagerApproval(user.id(), user.name(), user.role());
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            throw mapStatus(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw ApiException.serviceUnavailable("Le service d'identité est injoignable pour le moment.");
        }
    }

    private String extractMessage(RestClientResponseException ex) {
        try {
            LookupPinEnvelope error = ex.getResponseBodyAs(LookupPinEnvelope.class);
            return error != null && error.message() != null ? error.message() : "Code PIN invalide.";
        } catch (Exception parseFailure) {
            return "Code PIN invalide.";
        }
    }

    private ApiException mapStatus(HttpStatusCode status, String message) {
        return switch (status.value()) {
            case 400, 401, 422 -> ApiException.unauthorized(message);
            case 403 -> ApiException.forbidden(message);
            default -> ApiException.serviceUnavailable(message);
        };
    }

    private record LookupPinRequestBody(Long branchId, String pin) {
    }

    private record LookupPinEnvelope(String status, String message, UserSummary data) {
    }

    private record UserSummary(Long id, String name, String role) {
    }
}
