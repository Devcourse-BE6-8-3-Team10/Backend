package com.back.domain.admin.controller

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("AdminController 통합 테스트")
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Autowired
    private lateinit var postRepository: PostRepository

    private lateinit var testMember: Member
    private lateinit var testPost: Post

    @BeforeEach
    fun setUp() {
        // 테스트용 회원과 특허 데이터 설정
        testMember = memberRepository.findByEmail("user1@user.com").orElseThrow()
        testPost = postRepository.findByMember(testMember).stream().findFirst().orElseThrow()
    }

    @Test
    @DisplayName("전체 회원 목록 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getAllMembers_success() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,DESC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 목록 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageable.pageNumber").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageable.pageSize").value(10))
    }

    @Test
    @DisplayName("전체 회원 목록 조회 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user2@user.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getAllMembers_unauthorized() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @DisplayName("회원 상세 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getMemberDetail() {
        val member = memberRepository.findByEmail("user2@user.com").orElseThrow()

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members/{memberId}", member.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("user2@user.com"))
    }

    @Test
    @DisplayName("회원 상세 조회 실패 - 존재하지 않는 회원")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getMemberDetail_notFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members/{memberId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 회원입니다."))
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    fun updateMember_success() {
        // given
        val member = memberRepository.findByEmail("user2@user.com").orElseThrow()

        val request = AdminUpdateMemberRequest(
            "변경된이름",
            Status.BLOCKED,
            "https://new.image.url/profile.png"
        )

        // when
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/members/${member.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))

        // then
        val updated = memberRepository.findById(member.id).orElseThrow()
        Assertions.assertEquals("변경된이름", updated.name)
        Assertions.assertEquals(Status.BLOCKED, updated.status)
        Assertions.assertEquals("https://new.image.url/profile.png", updated.profileUrl)
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
    @WithUserDetails(value = "admin@admin.com")
    fun updateMember_fail_notFound() {
        // given
        val invalidId = 9999L
        val request = AdminUpdateMemberRequest(
            "아무거나",
            Status.ACTIVE,
            null
        )

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/members/$invalidId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 회원입니다."))
    }

    // ========== 특허 관련 테스트 시나리오 ==========
    @Test
    @DisplayName("전체 특허 목록 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getAllPatents_success() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/patents")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,DESC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 목록 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageable.pageNumber").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pageable.pageSize").value(10))
    }

    @Test
    @DisplayName("전체 특허 목록 조회 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user1@user.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getAllPatents_unauthorized() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/patents")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @DisplayName("특허 상세 조회 성공")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getPatentDetail_success() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/patents/{patentId}", testPost.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 정보 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(testPost.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value(testPost.getTitle()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorName").value(testPost.getMember().name))
    }

    @Test
    @DisplayName("특허 상세 조회 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com", userDetailsServiceBeanName = "customUserDetailsService")
    fun getPatentDetail_notFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/patents/{patentId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 특허입니다."))
    }

    @Test
    @DisplayName("특허 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    fun updatePatent_success() {
        // given
        val request = AdminUpdatePatentRequest(
            "수정된 특허 제목",
            "수정된 특허 설명입니다.",
            Post.Category.METHOD,
            200000,
            Post.Status.SALE
        )

        // when
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/patents/${testPost.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 정보 수정 성공"))

        // then
        val updated = postRepository.findById(testPost.id).orElseThrow()
        Assertions.assertEquals("수정된 특허 제목", updated.getTitle())
        Assertions.assertEquals("수정된 특허 설명입니다.", updated.getDescription())
        Assertions.assertEquals(Post.Category.METHOD, updated.getCategory())
        Assertions.assertEquals(200000, updated.getPrice())
        Assertions.assertEquals(Post.Status.SALE, updated.getStatus())
    }

    @Test
    @DisplayName("특허 정보 수정 실패 - 유효하지 않은 카테고리")
    @WithUserDetails(value = "admin@admin.com")
    fun updatePatent_fail_invalidCategory() {
        // given - JSON 문자열로 잘못된 카테고리 전송
        val invalidRequestJson = """
        {
            "title": "수정된 제목",
            "description": "수정된 설명",
            "category": "INVALID_CATEGORY",
            "price": 100000,
            "status": "SALE"
        }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/patents/${testPost.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("특허 정보 수정 실패 - 가격이 0 이하")
    @WithUserDetails(value = "admin@admin.com")
    fun updatePatent_fail_invalidPrice() {
        // given
        val request = AdminUpdatePatentRequest(
            "수정된 제목",
            "수정된 설명",
            Post.Category.PRODUCT,
            -1000,  // 유효하지 않은 가격
            Post.Status.SALE
        )

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/patents/${testPost.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("특허 삭제 성공")
    @WithUserDetails(value = "admin@admin.com")
    fun deletePatent_success() {
        // given
        val patentId = testPost.id

        // when
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/patents/$patentId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 삭제 성공"))

        // then
        assert(postRepository.findById(patentId).isEmpty())
    }

    @Test
    @DisplayName("특허 삭제 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com")
    fun deletePatent_fail_notFound() {
        // given
        val invalidId = 9999L

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/patents/$invalidId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("해당 특허가 존재하지 않습니다."))
    }

    @Test
    @DisplayName("특허 삭제 실패 - 관리자 권한 없음")
    @WithUserDetails(value = "user1@user.com")
    fun deletePatent_unauthorized() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/patents/${testPost.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }
}
