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

    /**
     * Finite, not {@code 0L} ("no timeout"): a client that disconnects without a clean TCP
     * close (network drop, laptop sleep, tab killed mid-stream — the common case, not the
     * exception) leaves its emitter's underlying socket write just... never returning, since
     * nothing ever tells Tomcat the peer is gone. With no timeout, {@link #broadcastToBranch}'s
     * blocking {@code emitter.send()} to that one zombie connection then hangs forever — and
     * since each broadcast runs on its own {@code @ApplicationModuleListener} async thread, every
     * *subsequent* order's notification hangs the exact same way on a brand-new thread, forever,
     * completely silently (no exception, nothing in the logs — confirmed via EVENT_PUBLICATION's
     * completion_date staying NULL across every order after the first stale connection). A finite
     * timeout makes Tomcat close the dead socket on its own, which turns that blocked write into
     * an IOException instead — caught below, connection dropped from the map, broadcast moves on.
     * The frontend already reconnects with a fresh ticket on any {@code EventSource} error/close
     * (see NotificationsProvider.tsx), so a periodic forced reconnect here is expected, not a bug.
     */
    private static final long CONNECTION_TIMEOUT_MS = 30 * 60 * 1000L;

    public SseEmitter register(Long branchId) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
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
