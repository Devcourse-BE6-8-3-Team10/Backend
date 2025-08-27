package com.back.domain.post.dto;

import com.back.domain.post.entity.Post;
import java.time.LocalDateTime;

//게시글 목록 응답용
public record PostListDTO(
        long id,
        String title,
        int price,
        String category,
        int favoriteCnt,
        LocalDateTime createdAt,
        String imageUrl
) {
    public PostListDTO(Post post) {
        this(
                post.getId(),
                post.getTitle(),
                post.getPrice(),
                post.getCategory().name(), // 영문 Enum 값으로 변경
                post.getFavoriteCnt(),
                post.getCreatedAt(),
                // postFiles 리스트가 비어있지 않으면 첫 번째 파일의 URL을, 비어있으면 null을 할당
                !post.getPostFiles().isEmpty() ? post.getPostFiles().get(0).getFileUrl() : null
        );
    }
}