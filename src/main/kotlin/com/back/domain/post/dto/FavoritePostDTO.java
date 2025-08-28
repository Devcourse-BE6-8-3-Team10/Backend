package com.back.domain.post.dto;

import com.back.domain.post.entity.FavoritePost;
import java.time.LocalDateTime;

//찜 목록 응답용
public record FavoritePostDTO(
        long postId,
        String title,
        int price,
        int favoriteCnt,
        String status,
        boolean isLiked,
        LocalDateTime createdAt
) {
    public FavoritePostDTO(FavoritePost favoritePost, boolean isLiked) {
        this(
                favoritePost.getPost().getId(),
                favoritePost.getPost().getTitle(),
                favoritePost.getPost().getPrice(),
                favoritePost.getPost().getFavoriteCnt(),
                favoritePost.getPost().getStatus().getLabel(),
                isLiked,
                favoritePost.getPost().getCreatedAt()
        );
    }
}


