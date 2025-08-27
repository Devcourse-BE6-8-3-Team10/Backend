package com.back.domain.post.controller;

import com.back.domain.post.dto.PostDetailDTO;
import com.back.domain.post.dto.PostListDTO;
import com.back.domain.post.dto.PostRequestDTO;
import com.back.domain.post.service.PostService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    //게시글 등록
    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<PostDetailDTO> createPost(@Valid @RequestBody PostRequestDTO dto) {
        PostDetailDTO result = postService.createPost(dto);
        URI location = URI.create("/api/posts/" + result.id());
        return ResponseEntity.created(location).body(result);
    }

    //게시글 수정
    @Operation(summary = "게시글 수정")
    @PatchMapping("/{postId}")
    public ResponseEntity<PostDetailDTO> updatePost(@PathVariable Long postId, @Valid @RequestBody PostRequestDTO dto) {
        PostDetailDTO result = postService.updatePost(postId, dto);
        return ResponseEntity.ok(result);
    }

    //게시글 삭제
    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<RsData<String>> deletePost(@PathVariable Long postId) {
        RsData<String> result = postService.deletePost(postId);
        return ResponseEntity.ok(result);
    }


    //게시글 목록 조회
    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<List<PostListDTO>> getPostList() {
        List<PostListDTO> result = postService.getPostList();
        return ResponseEntity.ok(result);
    }

    //게시글 상세 조회
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회")
    public RsData<PostDetailDTO> getPostDetail(@PathVariable Long postId) {
        return postService.getPostDetail(postId);
    }

    //인기 게시글 보여줌
    @Operation(summary = "인기 게시글 조회")
    @GetMapping("/popular")
    public ResponseEntity<List<PostListDTO>> getTop10PopularPosts() {
        List<PostListDTO> result = postService.getTop10PopularPosts();
        return ResponseEntity.ok(result);
    }

    // 내 게시글 목록 조회
    @Operation(summary = "내 게시글 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<List<PostListDTO>> getMyPosts() {
        List<PostListDTO> result = postService.getMyPosts();
        return ResponseEntity.ok(result);
    }

}