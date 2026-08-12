package com.monokek.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Plain reachability check — no auth, no DB/MinIO dependency. The Tauri
 * desktop app's {@code IPConfigModal} calls this before saving a server IP,
 * to confirm it's actually pointing at a monokek-spring instance. Already in
 * {@code SecurityConfig}'s public allow-list.
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
