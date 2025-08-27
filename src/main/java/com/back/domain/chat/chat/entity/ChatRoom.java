package com.back.domain.chat.chat.entity;

import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class ChatRoom extends BaseEntity {
    // 게시글 매니투원 설정
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    // 채팅방을 생성한 사용자 (매니투원)
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // 채팅방 이름 (자동 생성 또는 사용자 지정)
    private String roomName;

    // 메시지와의 관계 설정 (CASCADE로 ChatRoom 삭제 시 Message도 함께 삭제)
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages;

    // 참여자와의 관계 설정 (CASCADE로 ChatRoom 삭제 시 RoomParticipant도 함께 삭제)
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipant> participants;

    public ChatRoom(Post post, Member member) {
        this.post = post;
        this.member = member;
        // 채팅방 이름을 자동으로 생성 (게시글 제목 + 사용자명)
        this.roomName = post.getTitle() + " - " + member.getName();
    }

    public ChatRoom(Post post, Member member, String customRoomName) {
        this.post = post;
        this.member = member;
        this.roomName = customRoomName;
    }
}
