package com.meethybridhub.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP for audit/rate-limit records.
 *
 * X-Forwarded-For is only honored when {@code auth.rate-limit.trust-forwarded-header}
 * is enabled (i.e. the app sits behind OUR reverse proxy); trusting the raw
 * header by default would let anyone spoof it and rotate IPs past the per-IP
 * limit. When trusted, the LAST hop is used: X-Forwarded-For is appended by
 * each proxy, so the final element is the one OUR proxy added (the real
 * client). The first element is client-supplied and still spoofable.
 */
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
