package com.back.domain.post.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.dto.PostRequestDTO;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("PostController 통합 테스트")
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testUser;

    // 각 테스트 실행 전에 테스트용 유저를 조회해서 준비
    @BeforeEach
    void setUp() {
        testUser = memberRepository.findByEmail("user1@user.com").orElseThrow();
    }

    @Test
    @DisplayName("게시글 등록 성공")
    @WithUserDetails("user1@user.com") // "user1@user.com" 사용자로 로그인한 상태를 시뮬레이션
    void createPost_success() throws Exception {
        // given
        PostRequestDTO request = new PostRequestDTO(
                "새 게시글 제목",
                "새 게시글 내용입니다.",
                "PRODUCT",
                25000
        );

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 게시글 제목"))
                .andExpect(jsonPath("$.data.writerName").value(testUser.getName()))
                .andDo(print()); // 요청/응답 전체 내용 출력
    }

    @Test
    @DisplayName("게시글 상세 조회 성공")
    @WithUserDetails("user1@user.com")
    void getPostDetail_success() throws Exception {
        // given
        Post post = postRepository.save(Post.builder()
                .member(testUser)
                .title("테스트 게시글")
                .description("내용")
                .category(Post.Category.DESIGN)
                .price(100)
                .status(Post.Status.SALE)
                .build());

        // when & then
        mockMvc.perform(get("/api/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.title").value("테스트 게시글"));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    @WithUserDetails("user1@user.com")
    void getPostList_success() throws Exception {
        // given
        postRepository.save(Post.builder().member(testUser).title("제목1").category(Post.Category.PRODUCT).price(100).status(Post.Status.SALE).build());
        postRepository.save(Post.builder().member(testUser).title("제목2").category(Post.Category.DESIGN).price(200).status(Post.Status.SALE).build());

        // when & then
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2)) // 게시글이 2개인지 확인
                .andExpect(jsonPath("$.data[0].title").value("제목2")) // 최신순 정렬이므로 제목2가 먼저
                .andExpect(jsonPath("$.data[1].title").value("제목1"));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    @WithUserDetails("user1@user.com")
    void updatePost_success() throws Exception {
        // given
        Post post = postRepository.save(Post.builder()
                .member(testUser) // 게시글 작성자를 testUser로 설정
                .title("수정 전 제목")
                .category(Post.Category.PRODUCT)
                .price(100)
                .status(Post.Status.SALE)
                .build());

        PostRequestDTO request = new PostRequestDTO(
                "수정된 제목",
                "수정된 내용",
                "TRADEMARK",
                9999
        );

        // when & then
        mockMvc.perform(patch("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.category").value(Post.Category.TRADEMARK.getLabel()));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    @WithUserDetails("user1@user.com")
    void deletePost_success() throws Exception {
        // given
        Post post = postRepository.save(Post.builder()
                .member(testUser) // 게시글 작성자를 testUser로 설정
                .title("삭제될 게시글")
                .category(Post.Category.PRODUCT)
                .price(100)
                .status(Post.Status.SALE)
                .build());

        // when & then
        mockMvc.perform(delete("/api/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("게시글 삭제 완료"));

        // then - DB에서 실제로 삭제되었는지 확인
        assertThat(postRepository.findById(post.getId())).isEmpty();
    }
}