package com.back.domain.admin.dto.response;

import com.back.domain.post.entity.Post;

import java.time.LocalDateTime;

public record AdminPatentResponse(
    Long id,
    String title,
    String description,
    String category,
    Integer price,
    String status,
    Integer favoriteCnt,
    Long authorId,
    String authorName,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static AdminPatentResponse fromEntity(Post post) {
        return new AdminPatentResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getCategory().name(),
                post.getPrice(),
                post.getStatus().name(),
                post.getFavoriteCnt(),
                post.getMember().getId(),
                post.getMember().getName(),
                post.getCreatedAt(),
                post.getModifiedAt()
        );
    }
} 