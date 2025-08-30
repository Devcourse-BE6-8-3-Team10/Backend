package com.back.domain.chat.chat.entity

import com.back.domain.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class RoomParticipant(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    val chatRoom: ChatRoom,
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "member_id")
    val member: Member
) : BaseEntity() {
    
    var leftAt: LocalDateTime? = null
        private set
    
    var isActive: Boolean = true
        private set
    
    // 채팅방 나가기
    fun leave() {
        this.leftAt = LocalDateTime.now()
        this.isActive = false
    }
    
    // 채팅방 재참여
    fun rejoin() {
        this.leftAt = null
        this.isActive = true
    }
    
    // 활성화 (기존 채팅방 재사용시)
    fun activate() {
        this.isActive = true
        this.leftAt = null
    }
}
