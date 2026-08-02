package com.meethybridhub.api.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Smoke-test endpoint. It proves the whole vertical slice is wired correctly:
 * web layer, security filter chain, JSON serialization — and gives Swagger
 * something to render on day one.
 *
 * The package name is the convention we keep for the whole project:
 * "package by feature" (api.ping, later api.identity, api.store, api.order...)
 * rather than "package by layer" (controllers/services/repositories folders).
 * Feature packages keep every piece of one feature together — the industry
 * default for maintainable Spring applications.
 */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("meethybridhub", "pong", Instant.now());
    }

    /** A record used only as the response body — no mutable DTO class needed. */
    public record PingResponse(String service, String status, Instant timestamp) {
    }
}
