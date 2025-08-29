package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.entity.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<Message, Long> {

    fun findByChatRoomId(chatRoomId: Long): List<Message>

    // 채팅방의 마지막 메시지 조회 (생성일시 기준 내림차순 첫번째)
    //이건 null 가능
    fun findFirstByChatRoomIdOrderByCreatedAtDesc(chatRoomId: Long): Message?
}
