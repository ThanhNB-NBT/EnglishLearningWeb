package com.thanhnb.englishlearning.config;

import com.thanhnb.englishlearning.service.user.JwtBlacklistService;
import com.thanhnb.englishlearning.util.JwtUtil;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserDetailsService userDetailsService; 

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication cho public endpoints
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token từ Authorization header
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                // 1. Kiểm tra token có bị blacklist không (đã logout)
                if (jwtBlacklistService.isTokenBlacklisted(token)) {
                    log.debug("Token is blacklisted, request denied");
                    sendErrorResponse(response, "Token has been revoked");
                    return;
                }
                
                // 2. Validate JWT token
                if (jwtUtil.isTokenValid(token)) {
                    // Extract thông tin user từ JWT
                    String username = jwtUtil.getUsernameFromToken(token);
                    
                    // Set authentication context
                    setAuthenticationContext(username);
                    
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
            sendErrorResponse(response, "Authentication error");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token từ Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    /**
     * Set authentication context cho Spring Security 
     */
    private void setAuthenticationContext(String username) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, 
                    userDetails.getAuthorities() // Lấy authorities thực từ UserDetails
                );
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            log.error("Failed to load user details for: {}", username, e);
            throw new RuntimeException("User not found");
        }
    }

    /**
     * Gửi response lỗi
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    /**
     * Skip authentication cho các public endpoints
     * ⚠️ FIX: BỎ /api/auth/logout khỏi danh sách skip
     * → Logout PHẢI CẦN token để biết blacklist token nào
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/verify-email") ||
               path.startsWith("/api/auth/resend-verify-email") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/verify-reset-password") ||
               path.startsWith("/api/auth/endpoints") ||
               // ⚠️ BỎ logout khỏi đây - logout PHẢI CẦN authentication
               // path.startsWith("/api/auth/logout") || 
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/error"); 
    }
}