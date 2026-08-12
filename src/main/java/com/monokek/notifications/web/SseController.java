
package com.monokek.notifications.web;

import com.monokek.common.ApiException;
import com.monokek.notifications.application.SseConnectionRegistry;
import com.monokek.notifications.application.SseTicketService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * {@code /ticket} sits behind the normal JWT filter (real auth); {@code /stream}
 * is public in {@code SecurityConfig} and authorizes itself by consuming the
 * ticket — native {@code EventSource} can't send an {@code Authorization} header.
 */
@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SseTicketService ticketService;
    private final SseConnectionRegistry connectionRegistry;

    public SseController(SseTicketService ticketService, SseConnectionRegistry connectionRegistry) {
        this.ticketService = ticketService;
        this.connectionRegistry = connectionRegistry;
    }

    @PostMapping("/ticket")
    public Map<String, String> issueTicket(@RequestParam Long branchId) {
        return Map.of("ticket", ticketService.issue(branchId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        Long branchId = ticketService.consume(ticket)
                .orElseThrow(() -> ApiException.unauthorized("Ticket SSE invalide ou expiré"));
        return connectionRegistry.register(branchId);
    }
}
