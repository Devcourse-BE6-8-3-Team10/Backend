package com.back.domain.post.dto

import com.back.domain.post.entity.FavoritePost
import java.time.LocalDateTime

// 찜 목록 응답용
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
        favoritePost.post!!.id,
        favoritePost.post.title,
        favoritePost.post.price,
        favoritePost.post.favoriteCnt,
        favoritePost.post.status.label,
        isLiked,
        favoritePost.post.createdAt
    )
}
