package com.back.domain.chat.chat.entity;

import com.back.domain.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Setter
public class RoomParticipant extends BaseEntity {
    // room , user 메니투원 설정
    @ManyToOne
    private ChatRoom chatRoom;

    @ManyToOne
    private Member member;

    private LocalDateTime leftAt;

    private boolean isActive;

    public RoomParticipant(ChatRoom chatRoom, Member member) {
        this.chatRoom = chatRoom;
        this.member = member;
        this.isActive = true;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public Member getMember() {
        return member;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public boolean isActive() {
        return isActive;
    }
}
