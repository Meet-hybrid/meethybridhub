package com.meethybridhub.api.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;


@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("meethybridhub", "pong", Instant.now());
    }


    public record PingResponse(String service, String status, Instant timestamp) {
    }
}
