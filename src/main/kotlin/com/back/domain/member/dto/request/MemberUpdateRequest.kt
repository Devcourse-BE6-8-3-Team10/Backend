package com.back.domain.member.dto.request

import jakarta.validation.constraints.Size

data class MemberUpdateRequest(
    // null 이면 수정 안함
    val name: String?,

    // 비밀번호 변경 시에만 필요
    val currentPassword: String?,

    // null 이면 수정 안함
    @Size(min = 8, max = 20, message = "새 비밀번호는 8자 이상 20자 이하여야 합니다.")
    val newPassword: String?
)
