package com.back.global.security.config

import com.back.global.security.auth.CustomUserDetailsService
import com.back.global.security.jwt.JwtFilter
import com.back.global.security.jwt.JwtTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: CustomUserDetailsService
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 인증 없이 접근 가능한 경로들
                    .requestMatchers(
                        "/api/auth/signup", "/api/auth/login", "/api/auth/reissue",
                        "/api/posts", "/api/posts/popular", "/api/posts/{postId}", "/files/**"
                    ).permitAll()
                    // 비밀번호 찾기 관련 엔드포인트 허용
                    .requestMatchers("/api/members/verify-member", "/api/members/find-password").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    // Swagger 관련 경로들
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .requestMatchers("/swagger-resources/**").permitAll()
                    .requestMatchers("/webjars/**").permitAll()
                    // WebSocket 관련 경로들
                    .requestMatchers("/chat/**").permitAll()
                    .requestMatchers("/chat").permitAll()
                    .requestMatchers("/topic/**").permitAll()
                    .requestMatchers("/queue/**").permitAll()
                    .requestMatchers("/user/**").permitAll()
                    .requestMatchers("/app/**").permitAll()
                    // 정적 리소스
                    .requestMatchers("/favicon.ico").permitAll()
                    .requestMatchers("/*.html").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                    // 에러 경로 허용
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtFilter(jwtTokenProvider, userDetailsService),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .headers { it.frameOptions { frameOptions -> frameOptions.disable() } }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 허용할 오리진 설정 (개발 환경)
            allowedOrigins = mutableListOf(
                "http://localhost:3000",
                "http://34.64.160.179",
                "https://frontend-devteam-10.vercel.app/",
                "https://frontend-devteam-10.vercel.app",
                "https://www.devteam10.org"
            )
            // 허용할 HTTP 메서드 설정
            allowedMethods = mutableListOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            // 허용할 헤더 설정
            allowedHeaders = mutableListOf("*")
            // 인증 정보 포함 허용
            allowCredentials = true
            // 노출할 헤더 설정
            exposedHeaders = mutableListOf("Authorization", "Content-Type")
            // 프리플라이트 요청 캐시 시간 설정 (1시간)
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
