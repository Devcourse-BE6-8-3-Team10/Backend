package com.back.domain.trade.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.trade.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    // 거래 내역 조회: 구매자 또는 판매자로 필터링
    Page<Trade> findByBuyerOrSeller(Member buyer, Member seller, Pageable pageable);

    Optional<Trade> findFirstByOrderByCreatedAtDesc();
}