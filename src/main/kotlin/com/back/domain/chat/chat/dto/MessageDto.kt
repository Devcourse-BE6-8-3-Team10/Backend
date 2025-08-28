package com.back.domain.chat.chat.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class MessageDto @JsonCreator constructor(
    @JsonProperty("senderId") var senderId: Long = 0,
    @JsonProperty("chatRoomId") var chatRoomId: Long = 0,
    @JsonProperty("senderName") var senderName: String = "",
    @JsonProperty("senderEmail") var senderEmail: String = "",
    @JsonProperty("content") var content: String = "",
    @JsonProperty("messageType") var messageType: String = "MESSAGE"
) {

    // 빈 생성자 (기존 호환성을 위해)
    constructor() : this(0, 0, "", "", "", "MESSAGE")

    // 간단한 생성자 (기존 호환성을 위해)
    constructor(
        senderName: String,
        content: String,
        senderId: Long,
        chatRoomId: Long
    ) : this(senderId, chatRoomId, senderName, "", content, "MESSAGE")

    // 에러 메시지용 생성자
    constructor(
        senderName: String,
        content: String
    ) : this(-1L, -1L, senderName, "system@devteam10.org", content, "ERROR")

    // 기존 Java 코드 호환을 위한 setter 메서드들
    fun setSender(sender: String) {
        this.senderName = sender
    }

    fun getSender(): String = senderName
}
