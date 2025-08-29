package com.back.domain.chat.chat.controller

import com.back.domain.chat.chat.dto.ChatRoomDto
import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.chat.chat.service.ChatService
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/chat")
class ChatRestController(
    private val chatService: ChatService
) {

    @Operation(summary = "채팅 메시지 조회")
    @GetMapping("/rooms/{chatRoomId}/messages")
    fun getChatRoomMessages(
        @PathVariable chatRoomId: Long,
        principal: Principal
    ): RsData<List<MessageDto>> {
        val messageDtos = chatService.getChatRoomMessages(chatRoomId, principal)
        return RsData("200", "채팅방 메시지 조회 성공", messageDtos)
    }

    @Operation(summary = "채팅방 생성")
    @PostMapping("/rooms/{postId}")
    fun createChatRoom(
        @PathVariable postId: Long,
        principal: Principal
    ): RsData<Long> {
        val chatRoomId = chatService.createChatRoom(postId, principal.name)
        return RsData("200", "채팅방 생성 성공", chatRoomId)
    }

    @Operation(summary = "내가 속한 채팅방 목록 조회")
    @GetMapping("/rooms/my")
    fun getMyChatRooms(principal: Principal): RsData<List<ChatRoomDto>> {
        val chatRooms = chatService.getMyChatRooms(principal)
        return RsData("200", "내 채팅방 목록 조회 성공", chatRooms)
    }

    @Operation(summary = "채팅방 나가기")
    @DeleteMapping("/rooms/{chatRoomId}")
    fun leaveChatRoom(
        @PathVariable chatRoomId: Long,
        principal: Principal
    ): RsData<Unit> {
        chatService.leaveChatRoom(chatRoomId, principal)
        return RsData("200", "채팅방 나가기 성공")
    }
}
