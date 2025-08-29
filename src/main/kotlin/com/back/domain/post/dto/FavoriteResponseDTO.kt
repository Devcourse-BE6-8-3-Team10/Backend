package com.back.domain.post.dto

data class FavoriteResponseDTO(
    val postId: Long,
    val isLiked: Boolean,
    val favoriteCnt: Int,
    val message: String?
) 