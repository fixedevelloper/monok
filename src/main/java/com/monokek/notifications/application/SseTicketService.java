package com.monokek.notifications.application;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use tickets that stand in for the JWT on the SSE
 * stream endpoint, since native {@code EventSource} can't send an
 * {@code Authorization} header. {@code issue} runs behind the normal JWT
 * filter (real auth already happened); {@code consume} is what the public
 * stream endpoint calls to prove the connection was actually authorized.
 */
@Service
public class SseTicketService {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final Map<String, TicketInfo> tickets = new ConcurrentHashMap<>();
    private final Duration ttl;

    public SseTicketService() {
        this(DEFAULT_TTL);
    }

    /** Package-visible: lets tests use a very short TTL instead of waiting out the real 30s. */
    SseTicketService(Duration ttl) {
        this.ttl = ttl;
    }

    public String issue(Long branchId) {
        cleanupExpired();
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new TicketInfo(branchId, Instant.now().plus(ttl)));
        return ticket;
    }

    /** Single-use: the ticket is removed whether or not it's still valid. */
    public Optional<Long> consume(String ticket) {
        TicketInfo info = tickets.remove(ticket);
        if (info == null || Instant.now().isAfter(info.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(info.branchId());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        tickets.values().removeIf(info -> now.isAfter(info.expiresAt()));
    }

    private record TicketInfo(Long branchId, Instant expiresAt) {
    }
}
