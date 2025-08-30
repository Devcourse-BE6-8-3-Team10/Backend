package com.back.domain.admin.dto.response

import com.back.domain.post.entity.Post
import java.time.LocalDateTime

data class AdminPatentResponse(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val price: Int,
    val status: Post.Status,
    val favoriteCnt: Int,
    val authorId: Long,
    val authorName: String,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun fromEntity(post: Post): AdminPatentResponse {
            return AdminPatentResponse(
                id = post.id,
                title = post.title,
                description = post.description,
                category = post.category.name,
                price = post.price,
                status = post.status,
                favoriteCnt = post.favoriteCnt,
                authorId = post.member.id,
                authorName = post.member.name,
                createdAt = post.createdAt,
                modifiedAt = post.modifiedAt
            )
        }
    }
}