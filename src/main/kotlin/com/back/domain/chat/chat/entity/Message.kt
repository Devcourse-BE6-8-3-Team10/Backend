package com.back.domain.chat.chat.entity

import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class Message : BaseEntity {

    @ManyToOne
    var chatRoom: ChatRoom? = null  // private set 제거

    @ManyToOne
    var member: Member? = null

    var content: String = ""


    constructor()

    constructor(chatMessage: MessageDto, sender: Member) : this() {
        this.member = sender
        this.content = chatMessage.content
    }

    fun updateChatRoom(chatRoom: ChatRoom) {
        this.chatRoom = chatRoom
    }

    fun updateMember(member: Member) {
        this.member = member
    }

    fun updateContent(content: String) {
        this.content = content
    }
}
