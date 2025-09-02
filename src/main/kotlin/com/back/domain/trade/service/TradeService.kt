package com.back.domain.trade.service

import com.back.domain.member.entity.Member
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.repository.PostRepository
import com.back.domain.trade.dto.TradeDetailDto
import com.back.domain.trade.dto.TradeDto
import com.back.domain.trade.entity.Trade
import com.back.domain.trade.entity.TradeStatus
import com.back.domain.trade.repository.TradeRepository
import com.back.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TradeService (
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val tradeRepository: TradeRepository
) {
    //거래 생성
    @Transactional
    fun createTrade(postId: Long, buyerId: Long): Trade {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException("404-1", "게시글을 찾을 수 없습니다.") }

        if (post.member!!.id == buyerId) throw ServiceException("403-1", "자신의 게시글은 구매할 수 없습니다.")

        val buyer = memberRepository.findById(buyerId)
            .orElseThrow { ServiceException("404-2", "구매자를 찾을 수 없습니다.") }

        val trade = Trade(post, post.member, buyer, post.price, TradeStatus.COMPLETED)
        tradeRepository.save(trade)
        post.markAsSoldOut() // 판매 완료로 상태 변경
        return trade
    }

    //관리자 거래 전체 조회
    @Transactional(readOnly = true)
    fun getAllTrades(pageable: Pageable): Page<TradeDto> =
        tradeRepository.findAll(pageable).map { TradeDto(it) }

    //본인 거래 전체 조회
    @Transactional(readOnly = true)
    fun getMyTrades(member: Member, pageable: Pageable): Page<TradeDto> =
        tradeRepository.findByBuyerOrSeller(member, member, pageable).map { TradeDto(it) }

    //거래 상세 조회
    @Transactional(readOnly = true)
    fun getTradeDetail(id: Long, member: Member): TradeDetailDto {
        val trade = tradeRepository.findById(id).orElseThrow { ServiceException("404-1", "거래를 찾을 수 없습니다.") }

        if (trade.buyer.id != member.id && trade.seller.id != member.id) throw ServiceException("403-1", "본인의 거래만 조회할 수 있습니다.")

        return TradeDetailDto.from(trade)
    }

    //관리자 거래 상세 조회
    @Transactional(readOnly = true)
    fun getTradeDetailAsAdmin(tradeId: Long): TradeDetailDto = TradeDetailDto.from(
        tradeRepository.findById(tradeId).orElseThrow { ServiceException("404-1", "거래를 찾을 수 없습니다.") }
    )

    //최근 거래 조회
    @Transactional(readOnly = true)
    fun findLatest(): Trade = tradeRepository.findFirstByOrderByCreatedAtDesc()
        ?: throw NoSuchElementException("최근 거래 내역이 없습니다.")
}
