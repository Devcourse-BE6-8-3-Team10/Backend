package com.back.domain.auth.dto.request;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.entity.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberSignupRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        @NotBlank
        String name
) {
    public Member toEntity() {
        return new Member(email, password, name, null, Role.USER, Status.ACTIVE);
    }
}