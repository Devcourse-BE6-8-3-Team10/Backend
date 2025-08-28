package com.back.domain.chat.chat.dto;

public class CreateChatRoomRequestDto {
    private Long postId;
    private String roomName;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
}
