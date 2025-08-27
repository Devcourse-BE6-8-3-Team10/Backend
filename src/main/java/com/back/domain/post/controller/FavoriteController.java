package com.back.domain.post.controller;

import com.back.domain.post.dto.FavoriteResponseDTO;
import com.back.domain.post.dto.PostListDTO;
import com.back.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/likes")
public class FavoriteController {

    private final PostService postService;

    @Operation(summary = "찜 등록, 해제", description = "이미 찜한 경우 해제, 찜하지 않은 경우 등록")
    @PostMapping("/{postId}")
    public FavoriteResponseDTO toggleFavorite(@PathVariable Long postId) {
        return postService.toggleFavorite(postId);
    }

    @Operation(summary = "찜한 게시글 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<List<PostListDTO>> getFavoritePosts() {
        List<PostListDTO> result = postService.getFavoritePosts();
        return ResponseEntity.ok(result);
    }

}