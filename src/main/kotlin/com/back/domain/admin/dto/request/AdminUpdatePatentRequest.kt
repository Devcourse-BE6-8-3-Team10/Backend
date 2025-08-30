package com.back.domain.admin.dto.request

import com.back.domain.post.entity.Post
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class AdminUpdatePatentRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    val title: String,

    @field:NotBlank(message = "설명은 필수입니다.")
    val description: String,

    @field:NotNull(message = "카테고리는 필수입니다.")
    val category: Post.Category,

    @field:NotNull(message = "가격은 필수입니다.")
    @field:Positive(message = "가격은 0보다 커야 합니다.")
    val price: Int,

    @field:NotNull(message = "상태는 필수입니다.")
    val status: Post.Status
)