package com.trading.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rechaza peticiones cuyo header Host no esté en la lista permitida. Sin esto, un sitio web puede
 * reasociar su dominio a 127.0.0.1 (DNS rebinding) y leer la API local como si fuera del mismo origen,
 * esquivando CORS. Solo se acepta localhost/127.0.0.1 por defecto (web.allowed-hosts).
 */
@Component
public class HostValidationFilter extends OncePerRequestFilter {
    private final Set<String> allowedHosts;
    private final ObjectMapper objectMapper;

    public HostValidationFilter(
            @Value("${web.allowed-hosts}") String[] allowedHosts, ObjectMapper objectMapper) {
        this.allowedHosts = Arrays.stream(allowedHosts)
                .map(h -> h.trim().toLowerCase())
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toSet());
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String host = request.getHeader(HttpHeaders.HOST);
        // Sin header Host (clientes que no lo mandan, p. ej. MockMvc) no hay rebinding posible:
        // el navegador siempre manda Host, así que solo se rechaza un Host presente y no permitido.
        if (host == null || host.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        String hostname = host.split(":", 2)[0].trim().toLowerCase();
        if (!allowedHosts.contains(hostname)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    java.util.Map.of("error", java.util.Map.of(
                            "code", "HOST_NOT_ALLOWED",
                            "message", "Host no permitido"))));
            return;
        }
        chain.doFilter(request, response);
    }
}
