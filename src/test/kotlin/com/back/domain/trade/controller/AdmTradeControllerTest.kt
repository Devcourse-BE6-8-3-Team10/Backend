package com.back.domain.trade.controller

import com.back.domain.files.files.service.FileStorageService
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.repository.TradeRepository
import com.back.domain.trade.service.TradeService
import com.back.global.rq.Rq
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.config.http.MatcherType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
internal class AdmTradeControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val tradeService: TradeService,
    private val tradeRepository: TradeRepository,
    private val rq: Rq,
) {
    @MockBean lateinit var fileStorageService: FileStorageService

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("1. 전체 거래 목록 전체 조회 - 관리자")
    @Throws(Exception::class)
    fun t1() {
        // 1. 기대값 준비: DB에서 모든 거래 조회
        val pageable: Pageable = PageRequest.of(0, 20)
        val expectedPage: Page<TradeDto> = tradeService.getAllTrades(pageable)
        val trades = expectedPage.getContent()

        // 2. API 요청
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())

        // 3. 응답 기본 검증
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("전체 거래 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(trades.size))

        // 4. 실제 응답값 파싱
        val json = resultActions.andReturn().getResponse().getContentAsString()
        val contentArray = objectMapper.readTree(json).path("data").path("content")

        // 5. expected 목록을 id 기준으로 매핑
        val expectedMap = trades.stream()
            .collect(Collectors.toMap(TradeDto::id, Function.identity<TradeDto?>()))

        // 6. 응답 JSON 각각을 비교
        for (actual in contentArray) {
            val id = actual.get("id").asLong()
            val expected = expectedMap.get(id)

            Assertions.assertThat<TradeDto?>(expected).`as`("id=" + id + "인 거래가 실제 기대값에 없음").isNotNull()
            Assertions.assertThat(actual.get("postId").asLong()).isEqualTo(expected!!.postId)
            Assertions.assertThat(actual.get("sellerId").asLong()).isEqualTo(expected.sellerId)
            Assertions.assertThat(actual.get("buyerId").asLong()).isEqualTo(expected.buyerId)
            Assertions.assertThat(actual.get("price").asInt()).isEqualTo(expected.price)
            Assertions.assertThat(actual.get("status").asText()).isEqualTo(expected.status.name)
            Assertions.assertThat(actual.get("createdAt").asText()).isNotBlank()
        }
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("2. 전체 거래 목록 조회 - 일반 사용자")
    @Throws(Exception::class)
    fun t2() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("3. 거래 상세 조회 - 관리자")
    @Throws(Exception::class)
    fun t3() {
        val tradeId = 1L

        val trade = tradeRepository.findById(tradeId)
            .orElseThrow<RuntimeException?>(Supplier { RuntimeException("거래를 찾을 수 없습니다.") })

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/" + tradeId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("관리자 거래 상세 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(trade.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postId").value(trade.post.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postTitle").value(trade.post.getTitle()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.postCategory").value(trade.post.getCategory().getLabel()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.price").value(trade.price))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(trade.status.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.sellerEmail").value(trade.seller.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.buyerEmail").value(trade.buyer.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.createdAt").exists())
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("4. 거래 상세 조회 - 존재하지 않는 거래")
    @Throws(Exception::class)
    fun t4() {
        val nonExistentTradeId = 999L

        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/" + nonExistentTradeId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("거래를 찾을 수 없습니다."))
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("5. 거래 상세 조회 - 일반 사용자")
    @Throws(Exception::class)
    fun t5() {
        val tradeId = 1L

        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/trades/" + tradeId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }
}
