package com.back.global.security.jwt

import com.back.global.security.auth.CustomUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

// JWT 토큰으로 사용자 인증을 처리하는 필터
@Component
class JwtFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {
    
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        // 요청에서 토큰 추출 (Authorization 헤더 또는 쿠키에서)
        val token = resolveToken(request)

        // 토큰이 존재하고 유효한 경우 인증 처리
        if (token != null) {
            try {
                // 토큰 유효성 검증 (만료 여부, 서명 등)
                if (jwtTokenProvider.validateToken(token)) {
                    // 토큰에서 사용자 이메일 추출
                    val email = jwtTokenProvider.getEmailFromToken(token)

                    // 이메일로 UserDetails(Spring Security 사용자 정보 객체) 조회
                    val userDetails = userDetailsService.loadUserByUsername(email)

                    // UserDetails 기반으로 UsernamePasswordAuthenticationToken 생성
                    val auth = UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.authorities
                    )

                    // 인증 정보에 요청 세부 정보를 설정
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)

                    // SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (e: Exception) {
                // 로그 기록 후 인증 실패로 처리 (SecurityContextHolder는 이미 비어있음)
                logger.debug("JWT authentication failed", e)
            }
        }

        chain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 토큰 확인 (앱 환경용)
        val bearer = request.getHeader("Authorization")
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7)
        }

        // 2. 쿠키에서 토큰 확인 (웹 환경용, 자동 전송)
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if ("accessToken" == cookie.name) {
                    return cookie.value
                }
            }
        }

        return null
    }

    // 필터가 적용되지 않는 경로 설정 (Swagger, H2 콘솔 등)
    @Throws(ServletException::class)
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        for (prefix in EXCLUDED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true
            }
        }

        // HTML 파일들과 루트 경로
        if (path.endsWith(".html") || path == "/" || path == "/home") {
            return true
        }

        return false
    }

    companion object {
        // JWT 필터를 적용하지 않을 경로들
        private val EXCLUDED_PATH_PREFIXES = setOf(
            "/auth/", "/h2-console/", "/v3/api-docs", "/swagger-ui",
            "/swagger-resources", "/webjars/", "/ws/", "/chat/",
            "/topic/", "/app/", "/css/", "/js/", "/images/"
        )
    }
}