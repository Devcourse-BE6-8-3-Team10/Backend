package com.back.domain.auth.dto.request

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class MemberSignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    val name: String
) {
    // 현재는 Member Java Entity에 맞춰서 작성 -> 추후 코틀린 스타일로 변경 필요
    fun toEntity(): Member {
        return Member(email, password, name, null, Role.USER, Status.ACTIVE)
    }
}