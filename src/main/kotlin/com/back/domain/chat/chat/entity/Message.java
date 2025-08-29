package com.back.domain.chat.chat.entity;

import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor

public class Message extends BaseEntity {
    //member, chatroom 관계 설정
    @ManyToOne
    private ChatRoom chatRoom;

    @ManyToOne
    private Member sender;

    private String content;

    public Message(Member sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public Message(MessageDto chatMessage, Member sender) {
        this.sender = sender;
        this.content = chatMessage.getContent();
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public Member getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void setSender(Member sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
