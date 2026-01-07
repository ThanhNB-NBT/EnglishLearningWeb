package com.thanhnb.englishlearning.config;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserActivity;
import com.thanhnb.englishlearning.repository.user.UserActivityRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserActivityRepository activityRepository;

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
                    sendErrorResponse(response, "Token đã bị vô hiệu hóa");
                    return;
                }

                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.getUsernameFromToken(token);

                    Optional<User> userOpt = userRepository.findByUsername(username);
                    if (userOpt.isEmpty()) {
                        log.warn("Token for non-existent user: {}", username);
                        sendErrorResponse(response, "Tài khoản không tồn tại");
                        return;
                    }

                    User user = userOpt.get();

                    if (!user.getIsActive()) {
                        log.warn("Blocked user attempted access: {}", username);
                        sendErrorResponse(response, "Tài khoản đã bị khóa");
                        return;
                    }

                    if (user.getRole().name().equals("USER") && !user.getIsVerified()) {
                        log.warn("Unverified user attempted access: {}", username);
                        sendErrorResponse(response, "Tài khoản chưa được xác thực");
                        return;
                    }

                    Optional<UserActivity> activityOpt = activityRepository.findByUserId(user.getId());
                    if (activityOpt.isEmpty()) {
                        log.warn("User activity not found for user: {}", username);
                        sendErrorResponse(response, "Dữ liệu người dùng không hợp lệ");
                        return;
                    }

                    

                    UserActivity activity = activityOpt.get();

                    if (jwtUtil.isTokenIssuedBeforeLastUpdate(token, activity.getLastLoginDate())) {
                        log.warn("Token issued before last update for user: {}", username);
                        sendErrorResponse(response, "Phiên đăng nhập đã hết hạn");
                        return;
                    }

                    // ✅ Set authentication context
                    setAuthenticationContext(username, token, request.getRequestURI());
                    request.setAttribute("userId", user.getId());
                    // ✅ IMPROVED: Log successful authentication with details
                    log.debug("JWT authentication successful for user: {} | role: {} | path: {}", 
                        username, 
                        user.getRole(), 
                        request.getRequestURI());
                        
                } else {
                    log.debug("JWT token validation failed");
                    sendErrorResponse(response, "Token không hợp lệ hoặc đã hết hạn");
                    return;
                }
            } else {
                log.debug("No JWT token found in request");
                sendErrorResponse(response, "Thiếu token xác thực");
                return;
            }
        } catch (Exception e) {
            log.error("JWT authentication error on path {}: {}", 
                request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, "Lỗi xác thực");
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

    /**
     * ✅ Set authentication context with roles from JWT token
     * Spring Security will handle role checking via @PreAuthorize
     * 
     * @param username Username from token
     * @param token JWT token
     * @param requestPath Current request path (for logging)
     */
    private void setAuthenticationContext(String username, String token, String requestPath) {
        try {
            // Get roles from JWT token
            List<String> rolesFromToken = jwtUtil.getRolesFromToken(token);
            List<GrantedAuthority> authorities = rolesFromToken.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            // ✅ IMPROVED: Detailed logging
            log.debug("Setting auth for user: {} | roles: {} | authorities: {} | path: {}", 
                username, 
                rolesFromToken, 
                authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(", ")),
                requestPath);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // ✅ Create authentication with roles from JWT
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            // ✅ IMPROVED: Confirm authentication set
            log.debug("Authentication successfully set in SecurityContext for: {}", username);

        } catch (Exception e) {
            log.error("Failed to set authentication for: {} on path: {}", username, requestPath, e);
            throw new RuntimeException("Authentication setup failed");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"message\":\"%s\",\"statusCode\":401}",
                message));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();

        // ✅ IMPROVED: More detailed logging
        if (path == null) {
            log.warn("Request URI is null");
            return false;
        }

        boolean shouldSkip =
                path.startsWith("/media/") ||
                path.startsWith("/uploads/") ||
                path.startsWith("/api/debug/audio/") ||
                path.contains("/auth/user/login") ||
                path.contains("/auth/user/register") ||
                path.contains("/auth/user/verify-email") ||
                path.contains("/auth/user/resend-verify-email") ||
                path.contains("/auth/user/forgot-password") ||
                path.contains("/auth/user/verify-reset-password") ||
                path.contains("/auth/user/reset-password") ||
                path.contains("/auth/admin/login") ||
                path.contains("/auth/admin/forgot-password") ||
                path.contains("/auth/admin/verify-reset-password") ||
                path.contains("/auth/admin/reset-password") ||
                path.contains("/auth/teacher/login") ||
                path.contains("/auth/teacher/forgot-password") ||
                path.contains("/auth/teacher/verify-reset-password") ||
                path.contains("/auth/teacher/reset-password") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator") ||
                path.contains("/swagger-resources") ||
                path.contains("/webjars");

        if (shouldSkip) {
            log.trace("Skipping JWT filter for public endpoint: {}", path);
        } else {
            log.trace("Applying JWT filter for protected endpoint: {}", path);
        }

        return shouldSkip;
    }
}