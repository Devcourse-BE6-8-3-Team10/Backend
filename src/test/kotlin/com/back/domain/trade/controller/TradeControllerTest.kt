package com.back.domain.trade.controller

import com.back.domain.files.files.service.FileStorageService
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.entity.TradeStatus
import com.back.domain.trade.repository.TradeRepository
import com.back.domain.trade.service.TradeService
import com.back.global.rq.Rq
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.config.http.MatcherType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val tradeService: TradeService,
    private val tradeRepository: TradeRepository,
    private val rq: Rq,
    @MockBean private val fileStorageService: FileStorageService
) {

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("1. 거래 생성")
    fun t1() {
        val postId = 1L
        val content = objectMapper.writeValueAsString(mapOf("postId" to postId))

        val resultActions = mvc.perform(
            post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andDo(print())

        val trade = tradeService.findLatest()

        resultActions
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("${trade.id}번 거래가 생성되었습니다."))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.sellerId").value(trade.seller.id))
            .andExpect(jsonPath("$.data.buyerId").value(trade.buyer.id))
            .andExpect(jsonPath("$.data.price").value(trade.price))
            .andExpect(jsonPath("$.data.status").value(TradeStatus.COMPLETED.name))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }


    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("2. 거래 실패 - 게시글이 존재하지 않을 때")
    fun t2() {
        val postId = 99L
        val content = objectMapper.writeValueAsString(mapOf("postId" to postId))

        val resultActions = mvc.perform(
            post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andDo(print())

        resultActions
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("게시글을 찾을 수 없습니다."))
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("3. 거래 실패 - 자신의 게시글을 구매하려 할 때")
    fun t3() {
        val postId = 1L
        val content = objectMapper.writeValueAsString(mapOf("postId" to postId))

        val resultActions = mvc.perform(
            post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andDo(print())

        resultActions
            .andExpect(jsonPath("$.resultCode").value("403-1"))
            .andExpect(jsonPath("$.msg").value("자신의 게시글은 구매할 수 없습니다."))
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("4. 거래 실패 - 이미 판매된 게시글을 구매하려 할 때")
    fun t4() {
        val postId = 3L
        val content = objectMapper.writeValueAsString(mapOf("postId" to postId))

        val resultActions = mvc.perform(
            post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andDo(print())

        resultActions
            .andExpect(jsonPath("$.resultCode").value("400-3"))
            .andExpect(jsonPath("$.msg").value("이미 판매 완료된 게시글입니다."))
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("5. 내 거래 목록 전체 조회")
    fun t5() {
        // 1) 로그인 유저
        val loginUser = rq.member

        // 2) 기대값 조회 + DTO 변환
        val trades: List<TradeDto> =
            tradeRepository.findByBuyerOrSeller(loginUser, loginUser, Pageable.unpaged())
                .content
                .map { TradeDto(it) }

        // 3) 요청
        val resultActions = mvc.perform(
            get("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        // 4) 기본 검증
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("거래 목록 조회 성공"))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(trades.size))

        // 5) 응답 파싱
        val json = resultActions.andReturn().response.contentAsString
        val contentArray: JsonNode = objectMapper.readTree(json).path("data").path("content")

        // 6) 기대맵 (id 기준)
        val expectedMap: Map<Long, TradeDto> = trades.associateBy { it.id }

        // 7) 비교
        contentArray.forEach { actual ->
            val id = actual["id"].asLong()
            val expected = expectedMap[id]
            assertThat(expected).isNotNull

            assertThat(actual["postId"].asLong()).isEqualTo(expected!!.postId)
            assertThat(actual["sellerId"].asLong()).isEqualTo(expected.sellerId)
            assertThat(actual["buyerId"].asLong()).isEqualTo(expected.buyerId)
            assertThat(actual["price"].asInt()).isEqualTo(expected.price)
            assertThat(actual["status"].asText()).isEqualTo(expected.status.name)
            assertThat(actual["createdAt"].asText()).isNotBlank()
        }
    }

    @Test
    @WithUserDetails("user3@user.com")
    @DisplayName("6. 내 거래 목록 전체 조회 - 거래가 없는 경우")
    fun t6() {
        val resultActions = mvc.perform(
            get("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("거래 목록 조회 성공"))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("7. 거래 상세 조회")
    fun t7() {
        val tradeId = 1L
        val trade = tradeRepository.findById(tradeId)
            .orElseThrow { RuntimeException("거래를 찾을 수 없습니다.") }

        val resultActions = mvc.perform(
            get("/api/trades/$tradeId")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("거래 상세 조회 성공"))
            .andExpect(jsonPath("$.data.id").value(trade.id))
            .andExpect(jsonPath("$.data.postId").value(trade.post.id))
            .andExpect(jsonPath("$.data.postTitle").value(trade.post.title))
            .andExpect(jsonPath("$.data.postCategory").value(trade.post.category.label))
            .andExpect(jsonPath("$.data.price").value(trade.price))
            .andExpect(jsonPath("$.data.status").value(trade.status.name))
            .andExpect(jsonPath("$.data.sellerEmail").value(trade.seller.email))
            .andExpect(jsonPath("$.data.buyerEmail").value(trade.buyer.email))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    @WithUserDetails("user3@user.com")
    @DisplayName("8. 거래 상세 조회 - 본인이 아닌 거래 조회 시 실패")
    fun t8() {
        val tradeId = 1L

        val resultActions = mvc.perform(
            get("/api/trades/$tradeId")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
            .andExpect(jsonPath("$.msg").value("본인의 거래만 조회할 수 있습니다."))
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("9. 거래 상세 조회 - 존재하지 않는 거래 조회 시 실패")
    fun t9() {
        val tradeId = 99L

        val resultActions = mvc.perform(
            get("/api/trades/$tradeId")
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())

        resultActions
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("거래를 찾을 수 없습니다."))
    }
}