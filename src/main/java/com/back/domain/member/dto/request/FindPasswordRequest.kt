package com.back.domain.member.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FindPasswordRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String?,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String?,

    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    val newPassword: String?,

    val confirmPassword: String? = null
) 