package com.azurion.shared.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTrailFilter extends OncePerRequestFilter {

    private final AuditPersistenceService auditPersistenceService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long ms = Instant.now().toEpochMilli() - start.toEpochMilli();
            String tenant = Optional.ofNullable(MDC.get("tenant")).orElse("public");
            String user = Optional.ofNullable(MDC.get("userId")).orElse("anonymous");

            auditPersistenceService.save(
                    tenant,
                    user,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    ms,
                    "HTTP request audit"
            );
            log.info("Audit HTTP {} {} status={} ms={} tenant={} user={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    ms,
                    tenant,
                    user);
        }
    }
}
