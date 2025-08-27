package com.back.domain.chat.chat.dto;

public record ChatRoomDto (
    Long id,
    String name,
    Long postId,
    String lastContent
) {
    public static ChatRoomDto from(Long id, String name, Long postId, String lastContent) {
        return new ChatRoomDto(id, name, postId, lastContent);
    }
}
