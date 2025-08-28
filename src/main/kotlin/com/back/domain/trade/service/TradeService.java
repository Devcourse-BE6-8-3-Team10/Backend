package com.back.domain.trade.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.trade.dto.TradeDetailDto;
import com.back.domain.trade.dto.TradeDto;
import com.back.domain.trade.entity.Trade;
import com.back.domain.trade.entity.TradeStatus;
import com.back.domain.trade.repository.TradeRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final TradeRepository tradeRepository;

    //거래 생성
    @Transactional
    public Trade createTrade(Long postId, Long buyerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("404-1", "게시글을 찾을 수 없습니다."));

        if (post.getStatus() == Post.Status.SOLD_OUT) {
            throw new ServiceException("400-1", "이미 판매된 게시글입니다.");
        }

        if (post.getMember().getId().equals(buyerId)) {
            throw new ServiceException("403-1", "자신의 게시글은 구매할 수 없습니다.");
        }

        Member seller = post.getMember();
        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new ServiceException("404-2", "구매자를 찾을 수 없습니다."));

        Trade trade = new Trade(post, seller, buyer, post.getPrice(), TradeStatus.COMPLETED);
        tradeRepository.save(trade);

        post.markAsSoldOut(); // 판매 완료로 상태 변경

        return trade;
    }

    //관리자 거래 전체 조회
    @Transactional(readOnly = true)
    public Page<TradeDto> getAllTrades(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::new);
    }

    //본인 거래 전체 조회
    @Transactional(readOnly = true)
    public Page<TradeDto> getMyTrades(Member member, Pageable pageable) {
        return tradeRepository.findByBuyerOrSeller(member, member, pageable).map(TradeDto::new);
    }

    //거래 상세 조회
    @Transactional(readOnly = true)
    public TradeDetailDto getTradeDetail(Long id, Member member) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("404-1", "거래를 찾을 수 없습니다."));

        if (!trade.getBuyer().getId().equals(member.getId()) &&
                !trade.getSeller().getId().equals(member.getId())) {
            throw new ServiceException("403-1", "본인의 거래만 조회할 수 있습니다.");
        }

        return new TradeDetailDto(trade);
    }

    //관리자 거래 상세 조회
    @Transactional(readOnly = true)
    public TradeDetailDto getTradeDetailAsAdmin(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ServiceException("404-1", "거래를 찾을 수 없습니다."));

        return new TradeDetailDto(trade);
    }

    //최근 거래 조회
    @Transactional(readOnly = true)
    public Trade findLatest() {
        return tradeRepository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new NoSuchElementException("최근 거래 내역이 없습니다."));
    }
}