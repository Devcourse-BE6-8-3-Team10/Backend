package com.back.domain.chat.chat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class MessageDto {
    private Long senderId;
    private Long chatRoomId;

    @JsonProperty("senderName")
    private String senderName;
    @NonNull
    private String senderEmail;
    @NonNull
    private String content;
    private String messageType; // 메시지 타입 필드 추가 (일반 메시지, 나가기 알림 등)

    // Jackson JSON 역직렬화를 위한 sender 필드 (senderName과 동일)

//    private String sender;

    @JsonCreator
    public MessageDto(
            @JsonProperty("senderId") Long senderId,
            @JsonProperty("chatRoomId") Long chatRoomId,
            @JsonProperty("senderName") String senderName,
            @JsonProperty("senderEmail") String senderEmail,
            @JsonProperty("content") String content,
            @JsonProperty("messageType") String messageType) {
        this.senderId = senderId;
        this.chatRoomId = chatRoomId;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.content = content;
        this.messageType = messageType;
    }

    public MessageDto(String senderName, String content, Long senderId, Long chatRoomId) {
        this.senderName = senderName;
        this.content = content;
        this.senderId = senderId;
        this.chatRoomId = chatRoomId;
    }


    public void setSender(String sender) {
//        this.sender = sender;
        this.senderName = sender;
    }

    // 테스트 비교용 equals(), hashCode() 오버라이딩도 추천
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageDto)) return false;
        MessageDto messageDto = (MessageDto) o;
        return Objects.equals(senderId, messageDto.senderId) &&
                Objects.equals(content, messageDto.content) &&
                Objects.equals(chatRoomId, messageDto.chatRoomId) &&
                Objects.equals(senderEmail, messageDto.senderEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, content, chatRoomId, senderEmail);
    }
}
