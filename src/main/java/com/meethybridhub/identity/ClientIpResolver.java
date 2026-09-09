package com.meethybridhub.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class ClientIpResolver {

    @Value("${auth.rate-limit.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    public String resolve(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                return hops[hops.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
