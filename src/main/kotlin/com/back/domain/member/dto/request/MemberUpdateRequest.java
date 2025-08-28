package com.back.domain.member.dto.request;

public record MemberUpdateRequest(
        String name,
//        String profileUrl,
        String currentPassword,
        String newPassword
) {}
