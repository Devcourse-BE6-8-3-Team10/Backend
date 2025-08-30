package com.back.domain.auth.dto.response

data class TokenReissueResponse(
    @JvmField val accessToken: String,
    @JvmField val refreshToken: String
)
