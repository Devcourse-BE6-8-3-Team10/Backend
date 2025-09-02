package com.back.global.security.jwt

import com.back.domain.member.entity.Member
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

// JWT 토큰 생성 및 검증 담당
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-validity}") private val accessTokenValidity: Long,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {
    private val key: SecretKey

    init {
        this.key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
    }

    // Access Token 생성
    fun generateAccessToken(member: Member): String {
        val now = Date()
        return Jwts.builder()
            .setSubject(member.email)
            .claim("id", member.id)
            .claim("role", member.role.name)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + accessTokenValidity))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // Refresh Token 생성
    fun generateRefreshToken(member: Member): String {
        val now = Date()
        return Jwts.builder()
            .setSubject(member.email)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + refreshTokenValidity))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // 토큰에서 이메일 추출
    fun getEmailFromToken(token: String): String {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
                .subject
        } catch (e: JwtException) {
            throw RuntimeException("유효하지 않은 토큰입니다", e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("유효하지 않은 토큰입니다", e)
        }
    }

    // 토큰 유효성 검증
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
