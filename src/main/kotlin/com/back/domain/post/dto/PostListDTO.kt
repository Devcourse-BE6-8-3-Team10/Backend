package com.back.domain.post.dto

import com.back.domain.post.entity.Post
import java.time.LocalDateTime

// 게시글 목록 응답용
data class PostListDTO(
    val id: Long,
    val title: String,
    val price: Int,
    val category: String,
    val favoriteCnt: Int,
    val createdAt: LocalDateTime,
    val imageUrl: String?
) {
    companion object {
        // Post 엔티티를 PostListDTO로 변환하는 함수
        @JvmStatic
        fun of(post: Post): PostListDTO {
            return PostListDTO(
                id = post.id,
                title = post.title,
                price = post.price,
                category = post.category.name,
                favoriteCnt = post.favoriteCnt,
                createdAt = post.createdAt,
                imageUrl = post.postFiles.firstOrNull()?.fileUrl
            )
        }
    }
}