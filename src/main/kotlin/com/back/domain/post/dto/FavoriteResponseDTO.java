package com.back.domain.post.dto;

public record FavoriteResponseDTO(
        Long postId,
        boolean isLiked,
        int favoriteCnt,
        String message
) {}