package com.back.domain.trade.dto

import com.back.domain.trade.entity.Trade
import com.back.domain.trade.entity.TradeStatus
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class TradeDto(
    val id: Long,
    val postId: Long,
    val sellerId: Long,
    val buyerId: Long,
    val price: Int,
    val status: TradeStatus,
    @field:JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss",
        timezone = "Asia/Seoul"
    ) val createdAt: LocalDateTime
) {
    constructor(trade: Trade) : this(
        id = requireNotNull(trade.id),
        postId = requireNotNull(trade.post.id),
        sellerId = requireNotNull(trade.seller.id),
        buyerId = requireNotNull(trade.buyer.id),
        price = trade.price,
        status = trade.status,
        createdAt = trade.createdAt
    )
}
