package com.aichuangzuo.user.infrastructure.security;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/api/v1/user/internal/")
                || path.startsWith("/__test/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String jti = jwtUtil.getJti(token);
                    if (cacheUtil.get("user:auth:token-blacklist:" + jti) == null) {
                        Long userId = jwtUtil.parseAccessToken(token);
                        SecurityUserContext.setCurrentUserId(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        writeUnauthorized(response, UserAuthErrorCode.TOKEN_BLACKLISTED);
                        return;
                    }
                } catch (Exception e) {
                    writeUnauthorized(response, UserAuthErrorCode.TOKEN_INVALID);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, UserAuthErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(errorCode)));
    }
}
