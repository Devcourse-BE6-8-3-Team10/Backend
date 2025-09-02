package com.back.domain.auth.controller

import com.back.domain.auth.dto.request.MemberLoginRequest
import com.back.domain.auth.dto.request.MemberSignupRequest
import com.back.domain.auth.dto.request.TokenReissueRequest
import com.back.domain.auth.dto.response.MemberLoginResponse
import com.back.domain.auth.dto.response.TokenReissueResponse
import com.back.domain.auth.service.AuthService
import com.back.domain.member.dto.response.MemberInfoResponse
import com.back.domain.member.dto.response.MemberInfoResponse.Companion.fromEntity
import com.back.domain.member.service.MemberService
import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData
import com.back.global.security.auth.MemberDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val memberService: MemberService,
    private val authService: AuthService,
    @Value("\${jwt.access-token-validity}") private val accessTokenValidity: Long,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {

    // 회원가입 API
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    fun signup(@Valid @RequestBody request: MemberSignupRequest): ResponseEntity<RsData<String>> {
        memberService.signup(request)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "회원가입 성공")
        )
    }

    // 로그인 API
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    fun login(
        @Valid @RequestBody request: MemberLoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<RsData<MemberLoginResponse>> {
        val loginResponse = authService.login(request)

        // 토큰 쿠키 설정
        setTokenCookies(response, loginResponse.accessToken, loginResponse.refreshToken)

        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "로그인 성공", loginResponse)
        )
    }

    // 로그인 사용자 정보 조회 API
    @GetMapping("/me")
    @Operation(summary = "로그인 사용자 정보 조회", description = "유효한 JWT 토큰을 통해 현재 인증된 사용자 정보를 조회합니다.")
    fun me(authentication: Authentication?): ResponseEntity<RsData<MemberInfoResponse>> {
        val memberDetails = extractMemberDetails(authentication)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RsData(ResultCode.UNAUTHORIZED, "로그인된 사용자가 없습니다."))

        val response = fromEntity(memberDetails.member)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "사용자 정보 조회 성공", response)
        )
    }

    // 로그아웃 API
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자의 refresh 토큰을 삭제합니다.")
    fun logout(
        authentication: Authentication?,
        response: HttpServletResponse
    ): ResponseEntity<RsData<String>> {
        val memberDetails = extractMemberDetails(authentication)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RsData(ResultCode.UNAUTHORIZED, "로그인된 사용자가 없습니다."))

        authService.logout(memberDetails.member)

        // 쿠키 삭제
        clearTokenCookies(response)

        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "로그아웃 성공")
        )
    }

    // Access Token 재발급 API
    @PostMapping("/reissue")
    @Operation(summary = "AccessToken 재발급", description = "RefreshToken으로 AccessToken을 재발급 받습니다.")
    fun reissue(
        @Valid @RequestBody request: TokenReissueRequest,
        response: HttpServletResponse
    ): ResponseEntity<RsData<TokenReissueResponse>> {
        val reissueResponse = authService.reissueAccessToken(request)

        // 새로운 토큰 쿠키 설정
        setTokenCookies(response, reissueResponse.accessToken, reissueResponse.refreshToken)

        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "토큰 재발급 성공", reissueResponse)
        )
    }


    // 헬퍼 메서드들
    private fun extractMemberDetails(authentication: Authentication?): MemberDetails? {
        return if (authentication?.principal is MemberDetails) {
            authentication.principal as MemberDetails
        } else null
    }

    private fun setTokenCookies(response: HttpServletResponse, accessToken: String, refreshToken: String) {
        val accessTokenCookie = createCookie(
            name = "accessToken",
            value = accessToken,
            maxAge = (accessTokenValidity / 1000).toInt(),
            httpOnly = false,
            secure = false // TODO: 프로덕션에서는 true로 설정
        )

        val refreshTokenCookie = createCookie(
            name = "refreshToken",
            value = refreshToken,
            maxAge = (refreshTokenValidity / 1000).toInt(),
            httpOnly = true,
            secure = false // TODO: 프로덕션에서는 true로 설정
        )

        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())
    }

    private fun clearTokenCookies(response: HttpServletResponse) {
        val accessTokenCookie = createCookie("accessToken", "", 0, false, false)
        val refreshTokenCookie = createCookie("refreshToken", "", 0, true, false)

        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())
    }

    private fun createCookie(
        name: String,
        value: String,
        maxAge: Int,
        httpOnly: Boolean,
        secure: Boolean
    ): ResponseCookie {
        return ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(httpOnly)
            .secure(secure)
            .sameSite("Strict")
            .maxAge(maxAge.toLong())
            .build()
    }

}
