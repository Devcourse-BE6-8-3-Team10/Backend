package com.back.domain.trade.dto;


import com.back.domain.trade.entity.Trade;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NonNull;

import java.time.LocalDateTime;

public record TradeDto(
        @NonNull Long id,
        @NonNull Long postId,
        @NonNull Long sellerId,
        @NonNull Long buyerId,
        int price,
        @NonNull String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @NonNull LocalDateTime createdAt
) {
public TradeDto(Trade trade) {
        this(
            trade.getId(),
            trade.getPost().getId(),
            trade.getSeller().getId(),
            trade.getBuyer().getId(),
            trade.getPrice(),
            trade.getStatus().name(),
            trade.getCreatedAt()
        );
    }
}
