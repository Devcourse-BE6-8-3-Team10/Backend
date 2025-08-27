package com.back.domain.admin.dto.response;

import com.back.domain.member.entity.Member;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long id,
        String email,
        String name,
        String role,
        String profileUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt
) {
    public static AdminMemberResponse fromEntity(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name(),
                member.getProfileUrl(),
                member.getStatus().name(),
                member.getCreatedAt(),
                member.getModifiedAt(),
                member.getDeletedAt()
        );
    }
}