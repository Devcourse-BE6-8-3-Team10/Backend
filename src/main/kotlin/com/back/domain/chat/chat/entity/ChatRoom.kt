package com.back.domain.chat.chat.entity

import com.back.domain.member.entity.Member
import com.back.domain.post.entity.Post
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class ChatRoom : BaseEntity {

    @ManyToOne
    @JoinColumn(name = "post_id")
    var post: Post? = null
        private set

    @ManyToOne
    @JoinColumn(name = "member_id")
    var member: Member? = null
        private set

    var roomName: String? = null
        private set

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL], orphanRemoval = true)
    val messages: MutableList<Message> = mutableListOf()

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL], orphanRemoval = true)
    val participants: MutableList<RoomParticipant> = mutableListOf()

    constructor() // JPA 기본 생성자

    constructor(post: Post, member: Member) {
        this.post = post
        this.member = member
        // 채팅방 이름을 자동으로 생성 (게시글 제목 + 사용자명)
        this.roomName = "${post.title} - ${member.name}"
    }

    constructor(post: Post?, member: Member?, customRoomName: String?) {
        this.post = post
        this.member = member
        this.roomName = customRoomName
    }

    fun updatePost(post: Post) {
        this.post = post
    }

    fun updateMember(member: Member?) {
        this.member = member
    }

    fun updateRoomName(roomName: String) {
        this.roomName = roomName
    }

//    // Getter 메서드들 (Java 호환성을 위해)
//    fun getPost(): Post? = post
//    fun getMember(): Member? = member
//    fun getRoomName(): String? = roomName
//    fun getId(): Long? = super.id
}
