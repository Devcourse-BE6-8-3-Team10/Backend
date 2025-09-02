package com.back.domain.admin.controller

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.repository.TradeRepository
import com.back.domain.trade.service.TradeService
import com.back.global.rq.Rq
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

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

    @Autowired
    private lateinit var tradeService: TradeService

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    @Autowired
    private lateinit var rq: Rq

    private lateinit var testMember: Member
    private lateinit var testPost: Post

    @BeforeEach
    fun setUp() {
        testMember = memberRepository.findByEmail("user1@user.com").orElseThrow()
        testPost = postRepository.findByMember(testMember).stream().findFirst().orElseThrow()
    }

    // ========== 회원 관리 테스트 ==========

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
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 회원입니다."))
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    fun updateMember_success() {
        val member = memberRepository.findByEmail("user2@user.com").orElseThrow()

        val request = AdminUpdateMemberRequest(
            "변경된이름",
            Status.BLOCKED,
            "https://new.image.url/profile.png"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/members/${member.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))

        val updated = memberRepository.findById(member.id).orElseThrow()
        assertEquals("변경된이름", updated.name)
        assertEquals(Status.BLOCKED, updated.status)
        assertEquals("https://new.image.url/profile.png", updated.profileUrl)
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
    @WithUserDetails(value = "admin@admin.com")
    fun updateMember_fail_notFound() {
        val invalidId = 9999L
        val request = AdminUpdateMemberRequest(
            "아무거나",
            Status.ACTIVE,
            null
        )

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/members/$invalidId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 회원입니다."))
    }

    // ========== 특허 관리 테스트 ==========

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
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value(testPost.title))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.authorName").value(testPost.member.name))
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
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 특허입니다."))
    }

    @Test
    @DisplayName("특허 정보 수정 성공")
    @WithUserDetails(value = "admin@admin.com")
    fun updatePatent_success() {
        val request = AdminUpdatePatentRequest(
            "수정된 특허 제목",
            "수정된 특허 설명입니다.",
            Post.Category.METHOD,
            200000,
            Post.Status.SALE
        )

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/patents/${testPost.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 정보 수정 성공"))

        val updated = postRepository.findById(testPost.id).orElseThrow()
        assertEquals("수정된 특허 제목", updated.title)
        assertEquals("수정된 특허 설명입니다.", updated.description)
        assertEquals(Post.Category.METHOD, updated.category)
        assertEquals(200000, updated.price)
        assertEquals(Post.Status.SALE, updated.status)
    }

    @Test
    @DisplayName("특허 정보 수정 실패 - 유효하지 않은 카테고리")
    @WithUserDetails(value = "admin@admin.com")
    fun updatePatent_fail_invalidCategory() {
        val invalidRequestJson = """
        {
            "title": "수정된 제목",
            "description": "수정된 설명",
            "category": "INVALID_CATEGORY",
            "price": 100000,
            "status": "SALE"
        }
        """.trimIndent()

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
        val request = AdminUpdatePatentRequest(
            "수정된 제목",
            "수정된 설명",
            Post.Category.PRODUCT,
            -1000,
            Post.Status.SALE
        )

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
        val patentId = testPost.id

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/patents/$patentId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("특허 삭제 성공"))

        assertTrue(postRepository.findById(patentId).isEmpty())
    }

    @Test
    @DisplayName("특허 삭제 실패 - 존재하지 않는 특허")
    @WithUserDetails(value = "admin@admin.com")
    fun deletePatent_fail_notFound() {
        val invalidId = 9999L

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/patents/$invalidId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404"))
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

    // ========== 거래 관리 테스트 ==========

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("전체 거래 목록 전체 조회 - 관리자")
    fun getAllTrades_success() {
        val pageable: Pageable = PageRequest.of(0, 20)
        val expectedPage: Page<TradeDto> = tradeService.getAllTrades(pageable)
        val trades = expectedPage.content

        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("전체 거래 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(trades.size))

        val json = resultActions.andReturn().response.contentAsString
        val contentArray = objectMapper.readTree(json).path("data").path("content")

        val expectedMap = trades.stream()
            .collect(Collectors.toMap(TradeDto::id, Function.identity<TradeDto?>()))

        for (actual in contentArray) {
            val id = actual.get("id").asLong()
            val expected = expectedMap[id]

            assertThat(expected).`as`("id=$id 인 거래가 실제 기대값에 없음").isNotNull()
            assertThat(actual.get("postId").asLong()).isEqualTo(expected!!.postId)
            assertThat(actual.get("sellerId").asLong()).isEqualTo(expected.sellerId)
            assertThat(actual.get("buyerId").asLong()).isEqualTo(expected.buyerId)
            assertThat(actual.get("price").asInt()).isEqualTo(expected.price)
            assertThat(actual.get("status").asText()).isEqualTo(expected.status.name)
            assertThat(actual.get("createdAt").asText()).isNotBlank()
        }
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("전체 거래 목록 조회 - 일반 사용자")
    fun getAllTrades_unauthorized() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("거래 상세 조회 - 관리자")
    fun getTradeDetail_success() {
        val tradeId = 1L

        val trade = tradeRepository.findById(tradeId)
            .orElseThrow<RuntimeException>(Supplier { RuntimeException("거래를 찾을 수 없습니다.") })

        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/$tradeId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("관리자 거래 상세 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(trade.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").value(trade.post.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postTitle").value(trade.post.title))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postCategory").value(trade.post.category.label))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.price").value(trade.price))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(trade.status.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sellerEmail").value(trade.seller.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.buyerEmail").value(trade.buyer.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.createdAt").exists())
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("거래 상세 조회 - 존재하지 않는 거래")
    fun getTradeDetail_notFound() {
        val nonExistentTradeId = 999L

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/$nonExistentTradeId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("거래를 찾을 수 없습니다."))
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("거래 상세 조회 - 일반 사용자")
    fun getTradeDetail_unauthorized() {
        val tradeId = 1L

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/$tradeId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }
}