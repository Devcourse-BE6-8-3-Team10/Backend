package com.back.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

// 게시글 등록, 수정 요청용
data class PostRequestDTO(

        @field:NotBlank(message = "제목은 필수 입력 항목입니다.")
        val title: String,

        @field:NotBlank(message = "내용은 필수 입력 항목입니다.")
        val description: String,

        @field:NotBlank(message = "카테고리는 필수 입력 항목입니다.")
        val category: String,

        @field:Positive(message = "가격은 0보다 커야 합니다.")
        val price: Int
)