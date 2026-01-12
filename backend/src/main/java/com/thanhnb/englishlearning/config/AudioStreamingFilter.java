package com.thanhnb.englishlearning.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ✅ CRITICAL FIX: Enable proper CORS and Range support for audio streaming
 * 
 * Audio players need:
 * 1. CORS headers for cross-origin requests
 * 2. Accept-Ranges header to enable seeking
 * 3. Proper Content-Type
 */
@Component
@Slf4j
public class AudioStreamingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Only apply to media files
        if (path.startsWith("/media/")) {

            // ✅ CORS Headers
            String origin = req.getHeader("Origin");
            if (origin != null && (origin.equals("http://localhost:5173") ||
                    origin.equals("http://localhost:3000") ||
                    origin.equals("http://localhost:8980"))) {
                res.setHeader("Access-Control-Allow-Origin", origin);
                res.setHeader("Access-Control-Allow-Credentials", "true");
            }

            res.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Range, Content-Type, Accept");
            res.setHeader("Access-Control-Expose-Headers",
                    "Accept-Ranges, Content-Range, Content-Length, Content-Type");

            // ✅ Enable Range requests (critical for audio seeking)
            res.setHeader("Accept-Ranges", "bytes");

            // ✅ Cache control for better performance
            res.setHeader("Cache-Control", "public, max-age=3600");

            // Handle OPTIONS preflight
            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                res.setStatus(HttpServletResponse.SC_OK);
                log.debug("✅ CORS preflight handled for: {}", path);
                return;
            }

            log.debug("✅ Audio streaming headers set for: {}", path);
        }

        chain.doFilter(request, response);
    }
}