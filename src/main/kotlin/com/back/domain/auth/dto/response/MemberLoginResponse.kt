package com.back.domain.auth.dto.response

import com.back.domain.member.dto.response.MemberInfoResponse

@JvmRecord
data class MemberLoginResponse(
    @JvmField val accessToken: String?,
    @JvmField val refreshToken: String?,
    @JvmField val memberInfo: MemberInfoResponse?
)