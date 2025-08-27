package com.back.domain.trade.controller;

import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.entity.Member;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeControllerTest {
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
    @WithUserDetails("user2@user.com")
    @DisplayName("1. 거래 생성")
    void t1() throws Exception {
        Long postId = 1L;

        String content = objectMapper.writeValueAsString(Map.of("postId", postId));

        ResultActions resultActions = mvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andDo(print());

        Trade trade = tradeService.findLatest();

        resultActions
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 거래가 생성되었습니다.".formatted(trade.getId())))
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.sellerId").value(trade.getSeller().getId()))
                .andExpect(jsonPath("$.data.buyerId").value(trade.getBuyer().getId()))
                .andExpect(jsonPath("$.data.price").value(trade.getPrice()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("2. 거래 실패 - 게시글이 존재하지 않을 때")
    void t2() throws Exception {
        Long postId = 99L;

        String content = objectMapper.writeValueAsString(Map.of("postId", postId));

        ResultActions resultActions = mvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("게시글을 찾을 수 없습니다."));
    }

    @Test
    @WithUserDetails("user1@user.com")
    @DisplayName("3. 거래 실패 - 자신의 게시글을 구매하려 할 때")
    void t3() throws Exception {
        Long postId = 1L;

        String content = objectMapper.writeValueAsString(Map.of("postId", postId));

        ResultActions resultActions = mvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("자신의 게시글은 구매할 수 없습니다."));
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("4. 거래 실패 - 이미 판매된 게시글을 구매하려 할 때")
    void t4() throws Exception {
        Long postId = 3L;

        String content = objectMapper.writeValueAsString(Map.of("postId", postId));

        ResultActions resultActions = mvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("이미 판매된 게시글입니다."));
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("5. 내 거래 목록 전체 조회")
    void t5() throws Exception {
        // 1. 로그인된 사용자 정보 확보
        Member loginUser = rq.getMember();

        // 2. 실제 DB에서 기대값 조회 및 DTO 변환
        List<TradeDto> trades = tradeRepository.findByBuyerOrSeller(loginUser, loginUser, Pageable.unpaged())
                .stream()
                .map(TradeDto::new)
                .toList();

        // 3. API 요청
        ResultActions resultActions = mvc.perform(get("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        // 4. 응답 기본 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("거래 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(trades.size()));

        // 5. JSON 응답값 파싱
        String json = resultActions.andReturn().getResponse().getContentAsString();
        JsonNode contentArray = objectMapper.readTree(json).path("data").path("content");

        // 6. 기대값을 Map<Long, TradeDto>로 변환 (id 기준 매핑)
        Map<Long, TradeDto> expectedMap = trades.stream()
                .collect(Collectors.toMap(TradeDto::id, Function.identity()));

        // 7. 실제 응답과 기대값 비교
        for (JsonNode actual : contentArray) {
            Long id = actual.get("id").asLong();
            TradeDto expected = expectedMap.get(id);

            assertThat(expected).isNotNull();
            assertThat(actual.get("postId").asLong()).isEqualTo(expected.postId());
            assertThat(actual.get("sellerId").asLong()).isEqualTo(expected.sellerId());
            assertThat(actual.get("buyerId").asLong()).isEqualTo(expected.buyerId());
            assertThat(actual.get("price").asInt()).isEqualTo(expected.price());
            assertThat(actual.get("status").asText()).isEqualTo(expected.status());
            assertThat(actual.get("createdAt").asText()).isNotBlank();
        }
    }

    @Test
    @WithUserDetails("user3@user.com")
    @DisplayName("6. 내 거래 목록 전체 조회 - 거래가 없는 경우")
    void t6() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("거래 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("7. 거래 상세 조회")
    void t7() throws Exception {
        Long tradeId = 1L;

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다."));

        ResultActions resultActions = mvc.perform(get("/api/trades/" + tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("거래 상세 조회 성공"))
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
    @WithUserDetails("user3@user.com")
    @DisplayName("8. 거래 상세 조회 - 본인이 아닌 거래 조회 시 실패")
    void t8() throws Exception {
        Long tradeId = 1L;

        ResultActions resultActions = mvc.perform(get("/api/trades/" + tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("본인의 거래만 조회할 수 있습니다."));
    }

    @Test
    @WithUserDetails("user2@user.com")
    @DisplayName("9. 거래 상세 조회 - 존재하지 않는 거래 조회 시 실패")
    void t9() throws Exception {
        Long tradeId = 99L;

        ResultActions resultActions = mvc.perform(get("/api/trades/" + tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("거래를 찾을 수 없습니다."));
    }
}
