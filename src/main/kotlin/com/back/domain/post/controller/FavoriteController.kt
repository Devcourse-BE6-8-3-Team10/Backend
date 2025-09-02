package com.back.domain.post.controller

import com.back.domain.post.dto.FavoriteResponseDTO
import com.back.domain.post.dto.PostListDTO
import com.back.domain.post.service.PostService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/likes")
class FavoriteController(
    private val postService: PostService
) {

    @Operation(summary = "찜 등록, 해제", description = "이미 찜한 경우 해제, 찜하지 않은 경우 등록")
    @PostMapping("/{postId}")
    fun toggleFavorite(@PathVariable postId: Long): FavoriteResponseDTO {
        return postService.toggleFavorite(postId)
    }

    @Operation(summary = "찜한 게시글 목록 조회")
    @GetMapping("/me")
    fun getFavoritePosts(): ResponseEntity<List<PostListDTO>> {
        val result = postService.favoritePosts
        return ResponseEntity.ok(result)
    }
}
