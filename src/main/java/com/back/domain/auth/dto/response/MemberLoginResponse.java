package com.back.domain.auth.dto.response;

import com.back.domain.member.dto.response.MemberInfoResponse;

public record MemberLoginResponse(
        String accessToken,
        String refreshToken,
        MemberInfoResponse memberInfo
) {}