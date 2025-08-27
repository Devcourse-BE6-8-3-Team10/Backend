package com.back.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindPasswordRequest(
    @NotBlank(message = "이름은 필수입니다.")
    String name,

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    String newPassword,  // 선택적 필드로 변경

    String confirmPassword  // 선택적 필드로 변경
) {} 