package com.back.domain.trade.dto

import com.back.domain.trade.entity.Trade
import com.back.domain.trade.entity.TradeStatus
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

@JvmRecord
data class TradeDetailDto(
    val id: Long,
    val postId: Long,
    val postTitle: String,
    val postCategory: String,
    val price: Int,
    val status: TradeStatus,
     @param:JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss",
        timezone = "Asia/Seoul"
    ) val createdAt: LocalDateTime,
    val sellerEmail: String,
    val buyerEmail: String
){
companion object {
    @JvmStatic
    fun from(trade: Trade) = TradeDetailDto(
        id = trade.id,
        postId = trade.post.id,
        postTitle = trade.post.title,
        postCategory = trade.post.category.label,
        price = trade.price,
        status = trade.status,
        createdAt = trade.createdAt,
        sellerEmail = trade.seller.email,
        buyerEmail = trade.buyer.email
        )
    }
}
