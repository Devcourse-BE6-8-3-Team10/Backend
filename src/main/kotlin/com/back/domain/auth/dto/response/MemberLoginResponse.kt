package com.back.domain.auth.dto.response

import com.back.domain.member.dto.response.MemberInfoResponse

data class MemberLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val memberInfo: MemberInfoResponse
)