package com.back.domain.admin.dto.request

import com.back.domain.member.entity.Status
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AdminUpdateMemberRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,
    
    @field:NotNull(message = "상태는 필수입니다.")
    val status: Status,
    
    val profileUrl: String?
)