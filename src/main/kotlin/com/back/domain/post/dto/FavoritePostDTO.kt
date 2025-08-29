package com.back.domain.post.dto

import com.back.domain.post.entity.FavoritePost
import java.time.LocalDateTime

//찜 목록 응답용
data class FavoritePostDTO(
    val postId: Long,
    val title: String,
    val price: Int,
    val favoriteCnt: Int,
    val status: String,
    val isLiked: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        // 엔티티 객체로부터 DTO를 생성하는 팩토리 메서드
        @JvmStatic
        fun of(favoritePost: FavoritePost, isLiked: Boolean): FavoritePostDTO {
            val post = favoritePost.post
            return FavoritePostDTO(
                postId = post.id,
                title = post.title,
                price = post.price,
                favoriteCnt = post.favoriteCnt,
                status = post.status.label,
                isLiked = isLiked,
                createdAt = post.createdAt
            )
        }
    }
}