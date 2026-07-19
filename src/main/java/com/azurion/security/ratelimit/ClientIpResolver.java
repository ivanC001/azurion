package com.azurion.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    private final Set<String> trustedProxies;

    public ClientIpResolver(
            @Value("${azurion.security.rate-limit.trusted-proxies:127.0.0.1,::1}") String trustedProxies
    ) {
        this.trustedProxies = parseTrustedProxies(trustedProxies);
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalizeAddress(request.getRemoteAddr());
        if (!trustedProxies.contains(remoteAddress)) {
            return remoteAddress;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            for (int index = hops.length - 1; index >= 0; index--) {
                String candidate = normalizeAddress(hops[index]);
                if (!UNKNOWN.equals(candidate) && !trustedProxies.contains(candidate)) {
                    return candidate;
                }
            }
        }

        String realIp = normalizeAddress(request.getHeader("X-Real-IP"));
        return UNKNOWN.equals(realIp) ? remoteAddress : realIp;
    }

    private Set<String> parseTrustedProxies(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value != null) {
            Arrays.stream(value.split(","))
                    .map(this::normalizeAddress)
                    .filter(address -> !UNKNOWN.equals(address))
                    .forEach(result::add);
        }
        return Set.copyOf(result);
    }

    private String normalizeAddress(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        String address = value.trim().replace("\"", "");
        if (address.startsWith("[") && address.contains("]")) {
            address = address.substring(1, address.indexOf(']'));
        } else if (address.matches("^[0-9.]+:[0-9]+$")) {
            address = address.substring(0, address.lastIndexOf(':'));
        }
        if (address.isBlank() || address.length() > 64 || !address.matches("[0-9A-Fa-f:.]+")) {
            return UNKNOWN;
        }
        return address;
    }
}
