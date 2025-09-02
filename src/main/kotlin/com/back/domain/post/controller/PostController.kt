package com.back.domain.post.controller

import com.back.domain.post.dto.PostDetailDTO
import com.back.domain.post.dto.PostListDTO
import com.back.domain.post.dto.PostRequestDTO
import com.back.domain.post.service.PostService
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    @Operation(summary = "게시글 등록")
    @PostMapping
    fun createPost(@RequestBody @Valid dto: PostRequestDTO): ResponseEntity<PostDetailDTO> {
        val result = postService.createPost(dto)
        val location = URI.create("/api/posts/${result.id}")
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "게시글 수정")
    @PatchMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @RequestBody @Valid dto: PostRequestDTO
    ): ResponseEntity<PostDetailDTO> {
        val result = postService.updatePost(postId, dto)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long): ResponseEntity<RsData<String>> {
        val result = postService.deletePost(postId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    fun getPostList(): ResponseEntity<List<PostListDTO>> {
        val result = postService.postList
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    fun getPostDetail(@PathVariable postId: Long): RsData<PostDetailDTO> {
        return postService.getPostDetail(postId)
    }

    @Operation(summary = "인기 게시글 조회")
    @GetMapping("/popular")
    fun getTop10PopularPosts(): ResponseEntity<List<PostListDTO>> {
        val result = postService.top10PopularPosts
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "내 게시글 목록 조회")
    @GetMapping("/me")
    fun getMyPosts(): ResponseEntity<List<PostListDTO>> {
        val result = postService.myPosts
        return ResponseEntity.ok(result)
    }
}
