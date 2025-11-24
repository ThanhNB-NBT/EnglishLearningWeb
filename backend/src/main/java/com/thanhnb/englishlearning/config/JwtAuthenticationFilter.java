package com.thanhnb.englishlearning.config;

import com.thanhnb.englishlearning.service.user.JwtBlacklistService;
import com.thanhnb.englishlearning.util.JwtUtil;
import com.thanhnb.englishlearning.service.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                if (jwtBlacklistService.isTokenBlacklisted(token)) {
                    log.debug("Token is blacklisted, request denied");
                    sendErrorResponse(response, "Token has been revoked");
                    return;
                }

                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.getUsernameFromToken(token);
                    setAuthenticationContext(username, request);
                    log.debug("JWT authentication successful for user: {}", username);
                } else {
                    log.debug("JWT token validation failed");
                    sendErrorResponse(response, "Invalid or expired token");
                    return;
                }
            } else {
                log.debug("No JWT token found in request");
                sendErrorResponse(response, "Missing authentication token");
                return;
            }
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
            log.error("Critical Auth Error on path: {}", request.getRequestURI(), e);
            sendErrorResponse(response, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private void setAuthenticationContext(String username, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, 
                    userDetails.getAuthorities()
                );
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            try {
                Long userId = (Long) userDetails.getClass().getMethod("getId").invoke(userDetails);
                request.setAttribute("userId", userId);
            } catch (Exception e) {
                log.debug("Could not set userId attribute: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to load user details for: {}", username, e);
            throw new RuntimeException("User not found");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        log.info("Checking filter for path: {}", path);
        // Kiểm tra null safety
        if (path == null) return false;

        boolean shouldSkip = 
                // User Auth - Public
                path.contains("/auth/user/login") ||
                path.contains("/auth/user/register") ||
                path.contains("/auth/user/verify-email") ||
                path.contains("/auth/user/resend-verify-email") ||
                path.contains("/auth/user/forgot-password") ||
                path.contains("/auth/user/verify-reset-password") ||
                path.contains("/auth/user/reset-password") ||

                // Admin Auth - Public
                path.contains("/auth/admin/login") ||
                path.contains("/auth/admin/forgot-password") ||
                path.contains("/auth/admin/verify-reset-password") ||
                path.contains("/auth/admin/reset-password") ||

                // Swagger UI v3 (SpringDoc)
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator") ||
                path.contains("/swagger-resources") ||
                path.contains("/webjars");
        
        if (shouldSkip) {
            log.info("Skipping JWT filter for public endpoint: {}", path);
        } else {
            log.info("Applying JWT filter for protected endpoint: {}", path);
        }
        
        return shouldSkip;
    }
}