package com.back.domain.trade.repository

import com.back.domain.member.entity.Member
import com.back.domain.trade.entity.Trade
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TradeRepository : JpaRepository<Trade, Long> {
    // 거래 내역 조회: 구매자 또는 판매자로 필터링
    fun findByBuyerOrSeller(buyer: Member, seller: Member, pageable: Pageable): Page<Trade>

    fun findFirstByOrderByCreatedAtDesc(): Trade?
}