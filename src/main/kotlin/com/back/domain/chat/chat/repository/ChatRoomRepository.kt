package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.entity.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    //과연 채팅방이 null일수가 있는지? 일단 현재까지는 null 처리가 안되어 있는데 흐음..
    fun findByRoomName(roomName: String): ChatRoom?

    // 한개의 상품에 여러개의 채팅방이 있을 수 있음
    fun findByPostId(postId: Long): List<ChatRoom>

    // 특정 게시글에서 특정 사용자가 만든 채팅방 조회
    fun findByPostIdAndMemberId(postId: Long, memberId: Long): Optional<ChatRoom>

}
