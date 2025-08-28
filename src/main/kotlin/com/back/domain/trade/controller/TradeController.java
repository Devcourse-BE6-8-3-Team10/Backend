package com.back.domain.trade.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.trade.dto.TradeDetailDto;
import com.back.domain.trade.dto.TradeDto;
import com.back.domain.trade.dto.TradePageResponse;
import com.back.domain.trade.entity.Trade;
import com.back.domain.trade.service.TradeService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "TradeController", description = "거래 API 컨트롤러")
public class TradeController {
    private final TradeService tradeService;
    private final Rq rq;

    public record TradeCreateReqBody(
            @NotNull @Positive Long postId
    ) {
    }

    @PostMapping
    @Operation(summary = "거래 생성")
    public RsData<TradeDto> createTrade(@RequestBody @Valid TradeCreateReqBody reqBody) {
        Trade trade = tradeService.createTrade(reqBody.postId(), this.rq.getMember().getId());
        return new RsData<>(
                "201-1",
                "%s번 거래가 생성되었습니다.".formatted(trade.getId()),
                new TradeDto(trade)
        );
    }

    @GetMapping("")
    @Operation(summary = "본인 모든 거래 조회")
    public RsData<TradePageResponse<TradeDto>> getMyTrades(Pageable pageable) {
        Member member = rq.getMember();
        Page<TradeDto> trades = tradeService.getMyTrades(member, pageable);
        return new RsData<>(
                "200-1",
                "거래 목록 조회 성공",
                TradePageResponse.of(trades)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "거래 상세 조회")
    public RsData<TradeDetailDto> getTradeDetail(@PathVariable @Positive Long id) {
        return new RsData<>("200-1", "거래 상세 조회 성공", tradeService.getTradeDetail(id, rq.getMember()));
    }
}
