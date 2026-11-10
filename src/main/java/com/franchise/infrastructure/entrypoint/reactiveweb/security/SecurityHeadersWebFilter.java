package com.franchise.infrastructure.entrypoint.reactiveweb.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        applySecurityHeaders(exchange);
        return chain.filter(exchange);
    }

    public static void applySecurityHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/swagger-ui") && !path.startsWith("/webjars")) {
            headers.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }
    }
}
