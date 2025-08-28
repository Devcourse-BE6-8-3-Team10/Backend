package com.back.domain.auth.dto.request

import jakarta.validation.constraints.NotBlank

data class TokenReissueRequest(
    @field:NotBlank(message = "RefreshToken은 필수입니다.")
    val refreshToken: String
)