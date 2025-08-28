package com.back.domain.member.dto.response;

import com.back.domain.member.entity.Member;

public record OtherMemberInfoResponse(
        String name,
        String profileUrl
) {
    public static OtherMemberInfoResponse fromEntity(Member member) {
        return new OtherMemberInfoResponse(
                member.getName(),
                member.getProfileUrl()
        );
    }
}
