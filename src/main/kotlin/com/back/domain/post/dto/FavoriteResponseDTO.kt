package com.back.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty

@JvmRecord
data class FavoriteResponseDTO(
    val postId: Long,
    @get:JsonProperty("isLiked")
    val isLiked: Boolean,
    val favoriteCnt: Int,
    val message: String?
) 