package com.monokek.notifications.application;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active SSE connections, tagged by {@code branchId} so a broadcast only
 * reaches clients on the relevant branch. A dead connection is dropped via
 * {@code SseEmitter}'s own completion/timeout/error callbacks — no separate
 * heartbeat/liveness check needed.
 */
@Component
public class SseConnectionRegistry {

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public SseEmitter register(Long branchId) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L); // no timeout — a notification stream is meant to stay open
        connections.put(connectionId, new Connection(emitter, branchId));

        emitter.onCompletion(() -> connections.remove(connectionId));
        emitter.onTimeout(() -> connections.remove(connectionId));
        emitter.onError(e -> connections.remove(connectionId));

        try {
            // Spring/Tomcat don't commit the response (flush headers) until the first
            // write — without this, EventSource sits in "connecting" forever on a
            // branch with no activity, since it never sees the connection actually open.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcastToBranch(Long branchId, String eventName, Object payload) {
        connections.values().stream()
                .filter(connection -> connection.branchId().equals(branchId))
                .forEach(connection -> {
                    try {
                        connection.emitter().send(SseEmitter.event().name(eventName).data(payload));
                    } catch (IOException e) {
                        connection.emitter().completeWithError(e); // triggers onError -> removed from the map
                    }
                });
    }

    private record Connection(SseEmitter emitter, Long branchId) {
    }
}
