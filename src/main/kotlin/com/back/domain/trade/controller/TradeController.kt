package com.back.domain.trade.controller

import com.back.domain.trade.dto.TradeDetailDto
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.dto.TradePageResponse
import com.back.domain.trade.dto.TradePageResponse.Companion.of
import com.back.domain.trade.service.TradeService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trades")
@Tag(name = "TradeController", description = "거래 API 컨트롤러")
@Validated
class TradeController (
    private val tradeService: TradeService,
    private val rq: Rq
    ) {
    data class TradeCreateReqBody(
        @field:NotNull @field:Positive
        val postId: Long
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "거래 생성")
    fun createTrade(@RequestBody @Valid reqBody: TradeCreateReqBody): RsData<TradeDto> {
        val trade = tradeService.createTrade(reqBody.postId, rq.memberId)
        return RsData<TradeDto>(
            "201-1",
            "%s번 거래가 생성되었습니다.".format(trade.id),
            TradeDto(trade)
        )
    }

    @GetMapping
    @Operation(summary = "본인 모든 거래 조회")
    fun getMyTrades(pageable: Pageable): RsData<TradePageResponse<TradeDto>> {
        val trades: Page<TradeDto> = tradeService.getMyTrades(rq.member, pageable)
        return RsData<TradePageResponse<TradeDto>>(
            "200-1",
            "거래 목록 조회 성공",
            of(trades)
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "거래 상세 조회")
    fun getTradeDetail(@PathVariable @Positive id: Long): RsData<TradeDetailDto> =
        RsData<TradeDetailDto>("200-1", "거래 상세 조회 성공", tradeService.getTradeDetail(id, rq.member))
}