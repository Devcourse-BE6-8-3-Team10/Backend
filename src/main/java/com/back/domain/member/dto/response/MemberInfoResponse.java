package com.back.domain.member.dto.response;

import com.back.domain.member.entity.Member;

public record MemberInfoResponse(
        Long id,
        String email,
        String name,
        String role,
        String profileUrl
) {
    public static MemberInfoResponse fromEntity(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name(),
                member.getProfileUrl()
        );
    }
}