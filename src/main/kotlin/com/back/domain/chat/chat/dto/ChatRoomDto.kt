package com.back.domain.chat.chat.dto

data class ChatRoomDto(
    val id: Long,
    val name: String,
    val postId: Long,
    val lastContent: String
)
