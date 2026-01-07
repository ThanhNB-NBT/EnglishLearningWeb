package com.thanhnb.englishlearning.config;

import lombok.RequiredArgsConstructor;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.repository.user.UserActivityRepository;
import com.thanhnb.englishlearning.service.user.CustomUserDetailsService;
import com.thanhnb.englishlearning.service.user.JwtBlacklistService;
import com.thanhnb.englishlearning.util.JwtUtil;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtUtil jwtUtil;
        private final JwtBlacklistService jwtBlacklistService;
        private final CustomUserDetailsService userDetailsService;
        private final UserRepository userRepository;
        private final UserActivityRepository activityRepository;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(jwtUtil, jwtBlacklistService, userDetailsService, userRepository,
                                activityRepository);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ========== SYSTEM HEALTH CHECK ==========
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers("/swagger-ui/**").permitAll()
                                                .requestMatchers("/v3/api-docs/**").permitAll()
                                                .requestMatchers("/error").permitAll()

                                                // ========== MEDIA FILES ==========
                                                .requestMatchers("/media/**").permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/api/debug/audio/**").permitAll()

                                                // ========== PUBLIC ENDPOINTS ==========
                                                // User Auth - Public
                                                .requestMatchers("/api/auth/user/register").permitAll()
                                                .requestMatchers("/api/auth/user/login").permitAll()
                                                .requestMatchers("/api/auth/user/verify-email").permitAll()
                                                .requestMatchers("/api/auth/user/resend-verify-email").permitAll()
                                                .requestMatchers("/api/auth/user/forgot-password").permitAll()
                                                .requestMatchers("/api/auth/user/verify-reset-password").permitAll()
                                                .requestMatchers("/api/auth/user/reset-password").permitAll()

                                                // Admin Auth - Public
                                                .requestMatchers("/api/auth/admin/login").permitAll()
                                                .requestMatchers("/api/auth/admin/forgot-password").permitAll()
                                                .requestMatchers("/api/auth/admin/verify-reset-password").permitAll()
                                                .requestMatchers("/api/auth/admin/reset-password").permitAll()

                                                // Teacher Auth - Public
                                                .requestMatchers("/api/auth/teacher/login").permitAll()
                                                .requestMatchers("/api/auth/teacher/forgot-password").permitAll()
                                                .requestMatchers("/api/auth/teacher/verify-reset-password")
                                                .permitAll()
                                                .requestMatchers("/api/auth/teacher/reset-password").permitAll()

                                                // ========== ADMIN ONLY ENDPOINTS ==========
                                                // Account creation - Admin only
                                                .requestMatchers("/api/auth/admin/create").hasRole("ADMIN")
                                                .requestMatchers("/api/auth/teacher/create").hasRole("ADMIN")

                                                // Teacher assignment management - Admin only
                                                .requestMatchers("/api/admin/teachers/**").hasRole("ADMIN")

                                                .requestMatchers("/api/admin/topics/**")
                                                .hasAnyRole("ADMIN", "TEACHER")
                                                .requestMatchers("/api/admin/grammar/**")
                                                .hasAnyRole("ADMIN", "TEACHER")
                                                .requestMatchers("/api/admin/reading/**")
                                                .hasAnyRole("ADMIN", "TEACHER")
                                                .requestMatchers("/api/admin/listening/**")
                                                .hasAnyRole("ADMIN", "TEACHER")

                                                // ✅ NEW: Catch-all for other /api/admin/** paths - Admin only
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // ========== AUTHENTICATED LOGOUT ENDPOINTS ==========
                                                .requestMatchers("/api/auth/user/logout").authenticated()
                                                .requestMatchers("/api/auth/user/logout-all").authenticated()
                                                .requestMatchers("/api/auth/admin/logout").authenticated()
                                                .requestMatchers("/api/auth/admin/logout-all").authenticated()
                                                .requestMatchers("/api/auth/teacher/logout").authenticated()
                                                .requestMatchers("/api/auth/teacher/logout-all").authenticated()

                                                // ========== USER ENDPOINTS ==========
                                                .requestMatchers("/api/user/**").hasRole("USER")

                                                // ========== TEACHER ENDPOINTS ==========
                                                .requestMatchers("/api/teacher/**").hasRole("TEACHER")

                                                // All other requests need authentication
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:5173",
                                "http://localhost:3000",
                                "http://localhost:8080",
                                "*"));
                configuration.setAllowedMethods(Arrays.asList("*"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}