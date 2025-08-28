package com.back.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

//게시글 등록, 수정 요청용
public record PostRequestDTO(

        @NotBlank(message = "제목은 필수 입력 항목입니다.")
        String title,
        @NotBlank(message = "내용은 필수 입력 항목입니다.")
        String description,
        @NotBlank(message = "카테고리는 필수 입력 항목입니다.")
        String category,

        @Positive(message = "가격은 0보다 커야 합니다.")
        int price
) {}
