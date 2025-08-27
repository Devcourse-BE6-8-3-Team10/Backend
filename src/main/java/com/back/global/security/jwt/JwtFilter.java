package com.back.global.security.jwt;

import com.back.global.security.auth.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

// JWT 필터 클래스
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    // JWT 필터를 적용하지 않을 경로들
    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
            "/auth/", "/h2-console/", "/v3/api-docs", "/swagger-ui",
            "/swagger-resources", "/webjars/", "/ws/", "/chat/",
            "/topic/", "/app/", "/css/", "/js/", "/images/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 요청에서 토큰 추출 (Authorization 헤더 또는 쿠키에서)
        String token = resolveToken(request);

        // 토큰이 존재하고 유효한 경우 인증 처리
        if (token != null) {
            try {
                // 토큰이 유요한지 검증 (만료 여부, 서명 등)
                if (jwtTokenProvider.validateToken(token)) {

                    // 토큰에서 사용자 이메일 추출
                    String email = jwtTokenProvider.getEmailFromToken(token);

                    // 이메일로 UserDetails(Spring Security 사용자 정보 객체) 조회
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // UserDetails 기반으로 UsernamePasswordAuthenticationToken 생성
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // 인증 정보에 요청 세부 정보를 설정
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // 로그 기록 후 인증 실패로 처리 (SecurityContextHolder는 이미 비어있음)
                logger.debug("JWT authentication failed", e);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인 (앱 환경용)
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        
        // 2. 쿠키에서 토큰 확인 (웹 환경용, 자동 전송)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    // 필터가 적용 되지 않는 경로 설정 (Swagger, H2 콘솔 등)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        // HTML 파일들과 루트 경로
        if (path.endsWith(".html") || path.equals("/") || path.equals("/home")) {
            return true;
        }

        return false;
    }
}