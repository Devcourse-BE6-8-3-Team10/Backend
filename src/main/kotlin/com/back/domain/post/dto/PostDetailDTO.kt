package com.back.domain.post.dto

import com.back.domain.post.entity.Post
import java.time.LocalDateTime

//게시글 상세 조회 응답용
@JvmRecord
data class PostDetailDTO(
    @JvmField
    val id: Long,
    val writerName: String,
    val title: String,
    val description: String,
    val category: String,
    val price: Int,
    val status: String,
    val favoriteCnt: Int,
    val isLiked: Boolean,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    companion object {
        fun of(post: Post, isLiked: Boolean): PostDetailDTO {
            return PostDetailDTO(
                id = post.id,
                writerName = post.member.name,
                title = post.title,
                description = post.description,
                category = post.category.label,
                price = post.price,
                status = post.status.label,
                favoriteCnt = post.favoriteCnt,
                isLiked = isLiked,
                createdAt = post.createdAt,
                modifiedAt = post.modifiedAt
            )
        }
    }
}

