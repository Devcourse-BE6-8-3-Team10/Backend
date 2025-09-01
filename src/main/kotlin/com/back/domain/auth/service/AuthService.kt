package com.back.domain.auth.service

import com.back.domain.auth.dto.request.MemberLoginRequest
import com.back.domain.auth.dto.request.TokenReissueRequest
import com.back.domain.auth.dto.response.MemberLoginResponse
import com.back.domain.auth.dto.response.TokenReissueResponse
import com.back.domain.member.dto.response.MemberInfoResponse.Companion.fromEntity
import com.back.domain.member.entity.Member
import com.back.domain.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.ResultCode
import com.back.global.security.auth.MemberDetails
import com.back.global.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider
) {
    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
    }

    // 로그인
    @Transactional
    fun login(request: MemberLoginRequest): MemberLoginResponse {
        return try {
            // 1. 인증 시도
            val authToken = UsernamePasswordAuthenticationToken(request.email, request.password)
            val authentication = authenticationManager.authenticate(authToken)

            // 2. 인증 성공시 사용자 정보 로드
            val memberDetails = authentication.principal as? MemberDetails
                ?: throw ServiceException(ResultCode.SERVER_ERROR.code(), "인증 정보를 가져올 수 없습니다.")

            val member = memberDetails.member

            // 3. JWT 생성
            val accessToken = jwtTokenProvider.generateAccessToken(member)
            val refreshToken = jwtTokenProvider.generateRefreshToken(member)

            // 4. 리프레시 토큰 저장
            member.updateRefreshToken(refreshToken)
            saveMemberSafely(member, "토큰 저장에 실패했습니다.")

            // 5. DTO 응답 반환
            MemberLoginResponse(accessToken, refreshToken, fromEntity(member))
        } catch (e: BadCredentialsException) {
            throw ServiceException(ResultCode.INVALID_CREDENTIALS.code(), "이메일 또는 비밀번호가 잘못되었습니다.")
        }
    }

    // 로그아웃
    @Transactional
    fun logout(member: Member) {
        member.removeRefreshToken()
        memberRepository.save(member)
    }

    // Access Token 재발급
    @Transactional
    fun reissueAccessToken(request: TokenReissueRequest): TokenReissueResponse {
        val refreshToken = request.refreshToken

        // 1. 토큰 유효성 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ServiceException(
                ResultCode.INVALID_TOKEN.code(),
                "유효하지 않은 리프레시 토큰입니다."
            )
        }

        // 2. 이메일 추출 및 사용자 조회
        val email = jwtTokenProvider.getEmailFromToken(refreshToken)
        val member = memberRepository.findByEmail(email)
            .orElseThrow {
                ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "사용자를 찾을 수 없습니다.")
            }

        // 3. 토큰 일치 여부 확인
        validateRefreshToken(member, refreshToken)

        // 4. 새로운 토큰 생성
        val newAccessToken = jwtTokenProvider.generateAccessToken(member)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(member)

        // 5. 리프레시 토큰 갱신
        member.updateRefreshToken(newRefreshToken)
        saveMemberSafely(member, "토큰 갱신에 실패했습니다.")

        return TokenReissueResponse(newAccessToken, newRefreshToken)
    }

    // 헬퍼 함수들 - 시작
    private fun validateRefreshToken(member: Member, requestToken: String) {
        val storedToken = member.refreshToken
        when {
            storedToken == null -> throw ServiceException(
                ResultCode.INVALID_TOKEN.code(),
                "저장된 리프레시 토큰이 없습니다."
            )
            storedToken != requestToken -> throw ServiceException(
                ResultCode.INVALID_TOKEN.code(),
                "토큰이 서버와 일치하지 않습니다."
            )
        }
    }

    private fun saveMemberSafely(member: Member, errorMessage: String) {
        try {
            memberRepository.save(member)
        } catch (e: DataIntegrityViolationException) {
            log.error("Member 저장 실패: ${member.email}", e)
            throw ServiceException(ResultCode.SERVER_ERROR.code(), errorMessage)
        }
    }
    // 헬퍼 함수들 - 종료
}