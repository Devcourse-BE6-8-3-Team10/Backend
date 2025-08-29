package com.back.domain.trade.controller

import com.back.domain.trade.dto.TradeDetailDto
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.dto.TradePageResponse
import com.back.domain.trade.dto.TradePageResponse.Companion.of
import com.back.domain.trade.service.TradeService
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/trades")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AdmTradeController", description = "관리자용 거래 API 컨트롤러")
class AdmTradeController (
    private val tradeService: TradeService
) {
    @GetMapping
    @Operation(summary = "전체 거래 내역 조회")
    fun getAllTrades(
        @ParameterObject @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC
        ) pageable: Pageable
    ): RsData<TradePageResponse<TradeDto>> {
        val trades: Page<TradeDto> = tradeService.getAllTrades(pageable)
        return RsData<TradePageResponse<TradeDto>>("200-1", "전체 거래 조회 성공", of<TradeDto>(trades))
    }

    @GetMapping("/{id}")
    @Operation(summary = "관리자 거래 상세 조회")
    fun getTradeDetailAsAdmin(@PathVariable @Positive id: Long): RsData<TradeDetailDto> {
        val dto = tradeService.getTradeDetailAsAdmin(id)
        return RsData("200-1", "관리자 거래 상세 조회 성공", dto)
    }
}

