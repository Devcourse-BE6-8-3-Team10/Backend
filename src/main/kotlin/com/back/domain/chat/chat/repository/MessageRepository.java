package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomId(Long chatRoomId);
    
    // 채팅방의 마지막 메시지 조회 (생성일시 기준 내림차순 첫번째)
    Message findFirstByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
