package com.back.global.rq

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.global.security.auth.MemberDetails
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
class Rq(
    private val request: HttpServletRequest
) {
    
    /**
     * 현재 로그인된 사용자의 Member 객체를 반환
     */
    val member: Member?
        get() {
            val authentication = SecurityContextHolder.getContext().authentication
            return if (authentication?.principal is MemberDetails) {
                (authentication.principal as MemberDetails).member
            } else {
                null
            }
        }

    /**
     * 현재 로그인된 사용자의 ID를 반환
     */
    val memberId: Long?
        get() = member?.id

    /**
     * 현재 사용자의 로그인 상태 여부를 반환
     */
    val isLogin: Boolean
        get() = member != null

    /**
     * 현재 사용자의 역할(Role)이 ADMIN인지 여부를 반환
     */
    val isAdmin: Boolean
        get() = member?.role == Role.ADMIN
    
    /**
     * HttpServletRequest 객체 반환
     */
    fun getRequest(): HttpServletRequest = request
    
    /**
     * 요청 URI 반환
     */
    val requestURI: String
        get() = request.requestURI
    
    /**
     * 요청 메소드 반환
     */
    val method: String
        get() = request.method
}