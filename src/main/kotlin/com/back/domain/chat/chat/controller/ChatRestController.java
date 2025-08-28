package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private final ChatService chatService;

    @Operation(summary = "채팅 메시지 조회")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public RsData<List<MessageDto>> getChatRoomMessages(@PathVariable Long chatRoomId, Principal principal) {
        List<MessageDto> messageDtos = chatService.getChatRoomMessages(chatRoomId, principal);

        return new RsData<>("200", "채팅방 메시지 조회 성공", messageDtos);
    }

    @Operation(summary = "채팅방 생성")
    @PostMapping("/rooms/{postId}")
    public RsData<Long> createChatRoom(@PathVariable Long postId, Principal principal){
        Long chatRoomId = chatService.createChatRoom(postId, principal.getName());

        return new RsData<>("200", "채팅방 생성 성공", chatRoomId);
    }

    @Operation(summary = "내가 속한 채팅방 목록 조회")
    @GetMapping("/rooms/my")
    public RsData<List<ChatRoomDto>> getMyChatRooms(Principal principal) {
        List<ChatRoomDto> chatRooms = chatService.getMyChatRooms(principal);

        return new RsData<>("200", "내 채팅방 목록 조회 성공", chatRooms);
    }

    @Operation(summary = "채팅방 나가기")
    @DeleteMapping("/rooms/{chatRoomId}")
    public RsData<ChatRoomDto> leaveChatRoom(@PathVariable Long chatRoomId, Principal principal) {
        chatService.leaveChatRoom(chatRoomId, principal);

        return new RsData<>("200", "채팅방 나가기 성공");
    }


}
