package com.back.domain.trade.controller;

import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.trade.dto.TradeDto;
import com.back.domain.trade.entity.Trade;
import com.back.domain.trade.repository.TradeRepository;
import com.back.domain.trade.service.TradeService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdmTradeControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private Rq rq;
    @MockBean
    private FileStorageService fileStorageService;

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("1. 전체 거래 목록 전체 조회 - 관리자")
    void t1() throws Exception {
        // 1. 기대값 준비: DB에서 모든 거래 조회
        Pageable pageable = PageRequest.of(0, 20);
        Page<TradeDto> expectedPage = tradeService.getAllTrades(pageable);
        List<TradeDto> trades = expectedPage.getContent();

        // 2. API 요청
        ResultActions resultActions = mvc.perform(get("/api/admin/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        // 3. 응답 기본 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("전체 거래 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(trades.size()));

        // 4. 실제 응답값 파싱
        String json = resultActions.andReturn().getResponse().getContentAsString();
        JsonNode contentArray = objectMapper.readTree(json).path("data").path("content");

        // 5. expected 목록을 id 기준으로 매핑
        Map<Long, TradeDto> expectedMap = trades.stream()
                .collect(Collectors.toMap(TradeDto::id, Function.identity()));

        // 6. 응답 JSON 각각을 비교
        for (JsonNode actual : contentArray) {
            long id = actual.get("id").asLong();
            TradeDto expected = expectedMap.get(id);

            assertThat(expected).as("id=" + id + "인 거래가 실제 기대값에 없음").isNotNull();
            assertThat(actual.get("postId").asLong()).isEqualTo(expected.postId());
            assertThat(actual.get("sellerId").asLong()).isEqualTo(expected.sellerId());
            assertThat(actual.get("buyerId").asLong()).isEqualTo(expected.buyerId());
            assertThat(actual.get("price").asInt()).isEqualTo(expected.price());
            assertThat(actual.get("status").asText()).isEqualTo(expected.status());
            assertThat(actual.get("createdAt").asText()).isNotBlank();
        }
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("2. 전체 거래 목록 조회 - 일반 사용자")
    void t2() throws Exception {
        mvc.perform(get("/api/admin/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("3. 거래 상세 조회 - 관리자")
    void t3() throws Exception {
        Long tradeId = 1L;

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다."));

        ResultActions resultActions = mvc.perform(get("/api/admin/trades/" + tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("관리자 거래 상세 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(trade.getId()))
                .andExpect(jsonPath("$.data.postId").value(trade.getPost().getId()))
                .andExpect(jsonPath("$.data.postTitle").value(trade.getPost().getTitle()))
                .andExpect(jsonPath("$.data.postCategory").value(trade.getPost().getCategory().getLabel()))
                .andExpect(jsonPath("$.data.price").value(trade.getPrice()))
                .andExpect(jsonPath("$.data.status").value(trade.getStatus().name()))
                .andExpect(jsonPath("$.data.sellerEmail").value(trade.getSeller().getEmail()))
                .andExpect(jsonPath("$.data.buyerEmail").value(trade.getBuyer().getEmail()))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @WithUserDetails("admin@admin.com")
    @DisplayName("4. 거래 상세 조회 - 존재하지 않는 거래")
    void t4() throws Exception {
        Long nonExistentTradeId = 999L;

        mvc.perform(get("/api/admin/trades/" + nonExistentTradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("거래를 찾을 수 없습니다."));
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("5. 거래 상세 조회 - 일반 사용자")
    void t5() throws Exception {
        Long tradeId = 1L;

        mvc.perform(get("/api/admin/trades/" + tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
