package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    ChatRoom findByRoomName(String roomName);
    List<ChatRoom> findByPostId(Long postId); // 한개의 상품에 여러개의 채팅방이 있을 수 있음
    List<ChatRoom> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 특정 게시글에서 특정 사용자가 만든 채팅방 조회
    Optional<ChatRoom> findByPostIdAndMemberId(Long postId, Long memberId);

    // 특정 사용자가 만든 채팅방 목록 조회 (Principal용)
    List<ChatRoom> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
