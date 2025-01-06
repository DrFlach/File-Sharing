package com.example.TestProject.security;

import com.example.TestProject.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Пропускаем WebSocket handshake запросы
        return request.getRequestURI().startsWith("/ws");
    }

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100; // Высокий приоритет, но не самый высокий
    }

    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            logger.debug("Processing request URL: {} with Authorization header: {}",
                    request.getRequestURL(),
                    authorizationHeader != null ? "present" : "null");

            // Очищаем существующий контекст
            SecurityContextHolder.clearContext();

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                logger.debug("Processing JWT token");

                String email = jwtService.getEmailFromToken(token);
                logger.debug("Extracted username from token: {}", email);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

//                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                logger.debug("Loaded UserDetails for {}", username);

                    if (jwtService.validateToken(token, userDetails)) {
                        Claims claims = jwtService.extractClaims(token);
                        logger.debug("Token claims: {}", claims);

                        List<GrantedAuthority> authorities = Arrays.stream(claims.get("roles").toString().split(","))
                                .map(role -> {
                                    String formattedRole = role.trim().replace("_ROLE", "");
                                    return new SimpleGrantedAuthority("ROLE_" + formattedRole);
                                })
                                .collect(Collectors.toList());
                        logger.debug("Granted authorities: {}", authorities);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Создаем новый контекст безопасности
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);

                        logger.debug("Successfully authenticated user: {}", email);
                    } else {
                        logger.warn("Token validation failed for user: {}", email);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }

}