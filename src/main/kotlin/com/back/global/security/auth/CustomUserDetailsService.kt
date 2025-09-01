package com.back.global.security.auth

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
    override fun loadUserByUsername(email: String?): UserDetails {
        // 이메일로 회원 조회
        val member = memberRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.") }

        // 회원 상태 검증
        when (member.getStatus()) {
            Status.DELETED -> throw ResultCode.WITHDRAWN_MEMBER.toServiceException()
            Status.BLOCKED -> throw ResultCode.BLOCKED_MEMBER.toServiceException()
            else -> {}
        }

        return MemberDetails(member)
    }
}

// 예외처리 확장 함수
fun ResultCode.toServiceException(): ServiceException {
    return ServiceException(this.code(), this.message())
}
