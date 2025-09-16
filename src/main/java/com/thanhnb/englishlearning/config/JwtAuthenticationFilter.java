package com.thanhnb.englishlearning.config;

import com.thanhnb.englishlearning.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) 
            throws ServletException, IOException {

        String requestPath = request.getServletPath();
        
        logger.debug("Processing request: {} {}", request.getMethod(), requestPath);

        // Skip JWT processing cho public endpoints
        if (isPublicEndpoint(requestPath)) {
            logger.debug("Public endpoint, skipping JWT validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;

        // ✅ Kiểm tra header Authorization đúng cách
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                logger.debug("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                logger.error("Failed to extract username from JWT token", e);
            }
        } else {
            logger.debug("No valid Authorization header found");
        }

        // Xác thực JWT token nếu có username và chưa authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // Validate token
                if (jwtUtil.isTokenValid(jwt)) {
                    logger.debug("JWT token is valid for user: {}", username);
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    logger.warn("JWT token is invalid for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error during JWT authentication for user: {}", username, e);
            }
        }
        
        // LUÔN gọi filterChain.doFilter()
        filterChain.doFilter(request, response);
    }

    // Method kiểm tra public endpoints
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/verify-email") ||
               path.startsWith("/api/auth/resend-verify-email") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/verify-reset-password") ||
               path.startsWith("/api/auth/endpoints") ||
               path.startsWith("/api/users/endpoints") ||
               path.startsWith("/actuator") ||
               path.equals("/error");
    }
}
