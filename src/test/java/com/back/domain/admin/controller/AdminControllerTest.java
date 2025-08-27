package com.back.domain.admin.controller;

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest;
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Status;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("AdminController 통합 테스트")
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired
    private PostRepository postRepository;

    private Member testMember;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트용 회원과 특허 데이터 설정
        testMember = memberRepository.findByEmail("user1@user.com").orElseThrow();
        testPost = postRepository.findByMember(testMember).stream().findFirst().orElseThrow();
    }


    @Test
    @DisplayName("전체 회원 목록 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getAllMembers_success() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,DESC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10));
    }

    @Test
    @DisplayName("전체 회원 목록 조회 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user2@user.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getAllMembers_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("회원 상세 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getMemberDetail() throws Exception {
        Member member = memberRepository.findByEmail("user2@user.com").orElseThrow();

        mockMvc.perform(get("/api/admin/members/{memberId}", member.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user2@user.com"));
    }

    @Test
    @DisplayName("회원 상세 조회 실패 - 존재하지 않는 회원")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getMemberDetail_notFound() throws Exception {
        mockMvc.perform(get("/api/admin/members/{memberId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    void updateMember_success() throws Exception {
        // given
        Member member = memberRepository.findByEmail("user2@user.com").orElseThrow();

        AdminUpdateMemberRequest request = new AdminUpdateMemberRequest(
                "변경된이름",
                Status.BLOCKED,
                "https://new.image.url/profile.png"
        );

        // when
        mockMvc.perform(patch("/api/admin/members/" + member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));

        // then
        Member updated = memberRepository.findById(member.getId()).orElseThrow();
        assertEquals("변경된이름", updated.getName());
        assertEquals(Status.BLOCKED, updated.getStatus());
        assertEquals("https://new.image.url/profile.png", updated.getProfileUrl());
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
    @WithUserDetails(value = "admin@admin.com")
    void updateMember_fail_notFound() throws Exception {
        // given
        Long invalidId = 9999L;
        AdminUpdateMemberRequest request = new AdminUpdateMemberRequest(
                "아무거나",
                Status.ACTIVE,
                null
        );

        // when & then
        mockMvc.perform(patch("/api/admin/members/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 회원이 존재하지 않습니다."));
    }

    // ========== 특허 관련 테스트 시나리오 ==========

    @Test
    @DisplayName("전체 특허 목록 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getAllPatents_success() throws Exception {
        mockMvc.perform(get("/api/admin/patents")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,DESC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("특허 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10));
    }

    @Test
    @DisplayName("전체 특허 목록 조회 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user1@user.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getAllPatents_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/patents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("특허 상세 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getPatentDetail_success() throws Exception {
        mockMvc.perform(get("/api/admin/patents/{patentId}", testPost.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("특허 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(testPost.getId()))
                .andExpect(jsonPath("$.data.title").value(testPost.getTitle()))
                .andExpect(jsonPath("$.data.authorName").value(testPost.getMember().getName()));
    }

    @Test
    @DisplayName("특허 상세 조회 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    void getPatentDetail_notFound() throws Exception {
        mockMvc.perform(get("/api/admin/patents/{patentId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 특허입니다."));
    }

    @Test
    @DisplayName("특허 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    void updatePatent_success() throws Exception {
        // given
        AdminUpdatePatentRequest request = new AdminUpdatePatentRequest(
                "수정된 특허 제목",
                "수정된 특허 설명입니다.",
                "METHOD",
                200000,
                "SALE"
        );

        // when
        mockMvc.perform(patch("/api/admin/patents/" + testPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("특허 정보 수정 성공"));

        // then
        Post updated = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals("수정된 특허 제목", updated.getTitle());
        assertEquals("수정된 특허 설명입니다.", updated.getDescription());
        assertEquals(Post.Category.METHOD, updated.getCategory());
        assertEquals(200000, updated.getPrice());
    }

    @Test
    @DisplayName("특허 정보 수정 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com")
    void updatePatent_fail_notFound() throws Exception {
        // given
        Long invalidId = 9999L;
        AdminUpdatePatentRequest request = new AdminUpdatePatentRequest(
                "수정된 제목",
                "수정된 설명",
                "PRODUCT",
                100000,
                "SALE"
        );

        // when & then
        mockMvc.perform(patch("/api/admin/patents/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"))
                .andExpect(jsonPath("$.msg").value("해당 특허가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("특허 정보 수정 실패 - 유효하지 않은 카테고리")
    @WithUserDetails(value = "admin@admin.com")
    void updatePatent_fail_invalidCategory() throws Exception {
        // given
        AdminUpdatePatentRequest request = new AdminUpdatePatentRequest(
                "수정된 제목",
                "수정된 설명",
                "INVALID_CATEGORY",
                100000,
                "SALE"
        );

        // when & then
        mockMvc.perform(patch("/api/admin/patents/" + testPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 카테고리입니다."));
    }

    @Test
    @DisplayName("특허 삭제 성공")
    @WithUserDetails(value = "admin@admin.com")
    void deletePatent_success() throws Exception {
        // given
        Long patentId = testPost.getId();

        // when
        mockMvc.perform(delete("/api/admin/patents/" + patentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("특허 삭제 성공"));

        // then
        assert postRepository.findById(patentId).isEmpty();
    }

    @Test
    @DisplayName("특허 삭제 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com")
    void deletePatent_fail_notFound() throws Exception {
        // given
        Long invalidId = 9999L;

        // when & then
        mockMvc.perform(delete("/api/admin/patents/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"))
                .andExpect(jsonPath("$.msg").value("해당 특허가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("특허 삭제 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user1@user.com")
    void deletePatent_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/admin/patents/" + testPost.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}
