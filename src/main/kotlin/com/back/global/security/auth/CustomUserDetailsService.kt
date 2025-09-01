package com.back.global.security.auth

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.ResultCode
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val memberRepository: MemberRepository) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): UserDetails {
        // 이메일 정규화 및 유효성 검사
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            throw UsernameNotFoundException("인증에 실패했습니다.")
        }

        // 이메일로 회원 조회
        val member = memberRepository.findByEmail(normalizedEmail)
            .orElseThrow { UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.") }

        // 회원 상태 검증
        validateMemberStatus(member)

        return MemberDetails(member)
    }

    // 회원 상태 검증 메서드
    private fun validateMemberStatus(member: Member) {
        when (member.getStatus()) {
            Status.DELETED -> throw ResultCode.WITHDRAWN_MEMBER.toServiceException()
            Status.BLOCKED -> throw ResultCode.BLOCKED_MEMBER.toServiceException()
            Status.ACTIVE -> {} // 정상 상태 - 인증 진행
        }
    }
}

// 예외처리 확장 함수
fun ResultCode.toServiceException(): ServiceException {
    return ServiceException(this.code(), this.message())
}
