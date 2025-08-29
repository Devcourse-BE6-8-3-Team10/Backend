package com.back.domain.post.dto

import com.back.domain.post.entity.FavoritePost
import java.time.LocalDateTime

//찜 목록 응답용
@JvmRecord
data class FavoritePostDTO(
    val postId: Long,
    val title: String?,
    val price: Int,
    val favoriteCnt: Int,
    val status: String?,
    val isLiked: Boolean,
    val createdAt: LocalDateTime?
) {
    constructor(favoritePost: FavoritePost, isLiked: Boolean) : this(
        favoritePost.getPost().getId(),
        favoritePost.getPost().getTitle(),
        favoritePost.getPost().getPrice(),
        favoritePost.getPost().getFavoriteCnt(),
        favoritePost.getPost().getStatus().getLabel(),
        isLiked,
        favoritePost.getPost().getCreatedAt()
    )
}


