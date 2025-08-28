package com.back.domain.trade.controller;

import com.back.domain.trade.dto.TradeDetailDto;
import com.back.domain.trade.dto.TradeDto;
import com.back.domain.trade.dto.TradePageResponse;
import com.back.domain.trade.service.TradeService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/trades")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AdmTradeController", description = "관리자용 거래 API 컨트롤러")
public class AdmTradeController {

    private final TradeService tradeService;

    @GetMapping
    @Operation(summary = "전체 거래 내역 조회")
    public RsData<TradePageResponse<TradeDto>> getAllTrades(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<TradeDto> trades = tradeService.getAllTrades(pageable);
        return new RsData<>("200-1", "전체 거래 조회 성공", TradePageResponse.of(trades));
    }

    @GetMapping("/{id}")
    @Operation(summary = "관리자 거래 상세 조회")
    public RsData<TradeDetailDto> getTradeDetailAsAdmin(@PathVariable Long id) {
        TradeDetailDto dto = tradeService.getTradeDetailAsAdmin(id);
        return new RsData<>("200-1", "관리자 거래 상세 조회 성공", dto);
    }
}

