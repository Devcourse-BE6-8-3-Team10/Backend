package com.back.domain.member.dto.response;

import com.back.domain.member.entity.Member;

import java.time.LocalDateTime;

public record MemberMyPageResponse(
        Long id,
        String email,
        String name,
        String role,
        String profileUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemberMyPageResponse fromEntity(Member member) {
        return new MemberMyPageResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name(),
                member.getProfileUrl(),
                member.getStatus().name(),
                member.getCreatedAt(),
                member.getModifiedAt()
        );
    }
}