package com.back.domain.trade.dto;

import com.back.domain.trade.entity.Trade;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NonNull;

import java.time.LocalDateTime;

public record TradeDetailDto (
        @NonNull Long id,
        @NonNull Long postId,
        @NonNull String postTitle,
        @NonNull String postCategory,
        int price,
        @NonNull String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @NonNull LocalDateTime createdAt,
        @NonNull String sellerEmail,
        @NonNull String buyerEmail
){
    public TradeDetailDto(Trade trade) {
        this(
            trade.getId(),
            trade.getPost().getId(),
            trade.getPost().getTitle(),
            trade.getPost().getCategory().getLabel(),
            trade.getPrice(),
            trade.getStatus().name(),
            trade.getCreatedAt(),
            trade.getSeller().getEmail(),
            trade.getBuyer().getEmail()
        );
    }
}
