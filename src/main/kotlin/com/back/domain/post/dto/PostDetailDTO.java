package com.back.domain.post.dto;

import com.back.domain.post.entity.Post;
import java.time.LocalDateTime;

//게시글 상세 조회 응답용
public record PostDetailDTO(
        long id,
        String writerName,
        String title,
        String description,
        String category,
        int price,
        String status,
        int favoriteCnt,
        boolean isLiked,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public PostDetailDTO(Post post, boolean isLiked) {
        this(
                post.getId(),
                post.getMember().getName(),
                post.getTitle(),
                post.getDescription(),
                post.getCategory().getLabel(),
                post.getPrice(),
                post.getStatus().getLabel(),
                post.getFavoriteCnt(),
                isLiked,
                post.getCreatedAt(),
                post.getModifiedAt()
        );
    }
}

