package com.back.domain.trade.dto

import org.springframework.data.domain.Page

data class TradePageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
) {
    companion object {
        @JvmStatic
        fun <T> of(page: Page<T>): TradePageResponse<T> =
            TradePageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                last = page.isLast
            )
    }
}