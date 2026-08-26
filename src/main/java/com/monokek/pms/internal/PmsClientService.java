package com.monokek.pms.internal;

import com.monokek.common.ApiException;
import com.monokek.pms.PmsClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
class PmsClientService implements PmsClient {

    private final RestClient restClient;

    PmsClientService(@Value("${app.pms.api-url}") String pmsApiUrl) {
        // Bounded for the same reason as identity's HTTP clients: chargeToRoom() runs inline
        // inside OrderService#finalizePayment (before any local payment state is touched, see
        // its own doc) — the default request factory has no timeout at all, so a stalled/
        // unreachable pms-modulith would hang the cashier's "Encaisser" click indefinitely
        // instead of failing fast with "Le service hôtel (pms) est injoignable pour le moment.".
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(pmsApiUrl).requestFactory(requestFactory).build();
    }

    @Override
    public RoomCheckResult checkRoom(String roomNumber, String bearerToken) {
        RoomCheckResponse result = call(() -> restClient.get()
                .uri("/restaurant/check-room/{roomNumber}", roomNumber)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(RoomCheckResponse.class));
        return new RoomCheckResult(result.bookingId(), result.guestName());
    }

    @Override
    public void chargeToRoom(Long bookingId, BigDecimal amount, String orderReference, String bearerToken) {
        ChargeToRoomRequest body = new ChargeToRoomRequest(
                bookingId, "restaurant", "Commande monokek " + orderReference, 1, amount, orderReference);

        call(() -> restClient.post()
                .uri("/restaurant/charge")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Object.class));
    }

    /** Runs a pms-modulith call, translating its errors into the same {@link ApiException} the rest of this app already uses. */
    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            throw mapStatus(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw ApiException.serviceUnavailable("Le service hôtel (pms) est injoignable pour le moment.");
        }
    }

    private String extractMessage(RestClientResponseException ex) {
        try {
            PmsErrorResponse error = ex.getResponseBodyAs(PmsErrorResponse.class);
            return error != null && error.message() != null ? error.message() : ex.getMessage();
        } catch (Exception parseFailure) {
            return ex.getMessage();
        }
    }

    private ApiException mapStatus(HttpStatusCode status, String message) {
        return switch (status.value()) {
            case 404 -> ApiException.notFound(message);
            case 400, 422 -> ApiException.badRequest(message);
            case 401, 403 -> ApiException.forbidden(message);
            default -> ApiException.serviceUnavailable(message);
        };
    }
}
