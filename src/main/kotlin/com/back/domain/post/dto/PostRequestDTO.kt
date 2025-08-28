package com.back.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

//게시글 등록, 수정 요청용
@JvmRecord
data class PostRequestDTO(
    val title: @NotBlank(message = "제목은 필수 입력 항목입니다.") String?,
    val description: @NotBlank(message = "내용은 필수 입력 항목입니다.") String?,
    val category: @NotBlank(message = "카테고리는 필수 입력 항목입니다.") String?,

    val price: @Positive(message = "가격은 0보다 커야 합니다.") Int
)
