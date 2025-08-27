package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRestController.class)
@ActiveProfiles("test")
@DisplayName("ChatRestController 통합 테스트")
class ChatRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<MessageDto> testMessages;
    private List<ChatRoomDto> testChatRooms;

    @BeforeEach
    void setUp() {
        // 테스트 메시지 데이터 생성
        MessageDto message1 = new MessageDto("사용자1", "안녕하세요", 1L, 1L);
        MessageDto message2 = new MessageDto("사용자2", "안녕히가세요", 2L, 1L);
        testMessages = Arrays.asList(message1, message2);

        // 테스트 채팅방 데이터 생성
        ChatRoomDto chatRoom1 = new ChatRoomDto(1L, "테스트 게시글 - 사용자1", 1L, "안녕하세요");
        ChatRoomDto chatRoom2 = new ChatRoomDto(2L, "다른 게시글 - 사용자1", 2L, "감사합니다");
        testChatRooms = Arrays.asList(chatRoom1, chatRoom2);
    }

    @Nested
    @DisplayName("채팅방 메시지 조회 API 테스트")
    class GetChatRoomMessagesTest {

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("채팅방 메시지 조회 성공")
        void getChatRoomMessages_Success() throws Exception {
            // Given
            Long chatRoomId = 1L;
            given(chatService.getChatRoomMessages(eq(chatRoomId), any(Principal.class)))
                    .willReturn(testMessages);

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.message").value("채팅방 메시지 조회 성공"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].content").value("안녕하세요"))
                    .andExpect(jsonPath("$.data[1].content").value("안녕히가세요"));
        }

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("존재하지 않는 채팅방 조회 시 404 에러")
        void getChatRoomMessages_NotFound() throws Exception {
            // Given
            Long chatRoomId = 999L;
            given(chatService.getChatRoomMessages(eq(chatRoomId), any(Principal.class)))
                    .willThrow(new ServiceException("404-4", "존재하지 않는 채팅방입니다."));

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("권한이 없는 채팅방 조회 시 403 에러")
        void getChatRoomMessages_Forbidden() throws Exception {
            // Given
            Long chatRoomId = 1L;
            given(chatService.getChatRoomMessages(eq(chatRoomId), any(Principal.class)))
                    .willThrow(new ServiceException("403-1", "채팅방 참여자만 메시지를 조회할 수 있습니다."));

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("채팅방 생성 API 테스트")
    class CreateChatRoomTest {

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("채팅방 생성 성공")
        void createChatRoom_Success() throws Exception {
            // Given
            Long postId = 1L;
            Long createdChatRoomId = 10L;
            given(chatService.createChatRoom(eq(postId), any(String.class)))
                    .willReturn(createdChatRoomId);

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.message").value("채팅방 생성 성공"))
                    .andExpect(jsonPath("$.data").value(createdChatRoomId));
        }

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("존재하지 않는 게시글로 채팅방 생성 시 404 에러")
        void createChatRoom_PostNotFound() throws Exception {
            // Given
            Long postId = 999L;
            given(chatService.createChatRoom(eq(postId), any(String.class)))
                    .willThrow(new ServiceException("404-1", "존재하지 않는 게시글입니다."));

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 생성 시 401 에러")
        void createChatRoom_Unauthorized() throws Exception {
            // Given
            Long postId = 1L;

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회 API 테스트")
    class GetMyChatRoomsTest {

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("내 채팅방 목록 조회 성공")
        void getMyChatRooms_Success() throws Exception {
            // Given
            given(chatService.getMyChatRooms(any(Principal.class)))
                    .willReturn(testChatRooms);

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/my")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.message").value("내 채팅방 목록 조회 성공"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].roomName").value("테스트 게시글 - 사용자1"))
                    .andExpect(jsonPath("$.data[1].roomName").value("다른 게시글 - 사용자1"));
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 목록 조회 시 401 에러")
        void getMyChatRooms_Unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/chat/rooms/my")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 API 테스트")
    class LeaveChatRoomTest {

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("채팅방 나가기 성공")
        void leaveChatRoom_Success() throws Exception {
            // Given
            Long chatRoomId = 1L;
            doNothing().when(chatService).leaveChatRoom(eq(chatRoomId), any(Principal.class));

            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.message").value("채팅방 나가기 성공"));
        }

        @Test
        @WithMockUser(username = "test@test.com")
        @DisplayName("참여하지 않은 채팅방 나가기 시 404 에러")
        void leaveChatRoom_NotParticipant() throws Exception {
            // Given
            Long chatRoomId = 1L;
            doThrow(new ServiceException("404-5", "채팅방 참여자가 아닙니다."))
                    .when(chatService).leaveChatRoom(eq(chatRoomId), any(Principal.class));

            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 나가기 시 401 에러")
        void leaveChatRoom_Unauthorized() throws Exception {
            // Given
            Long chatRoomId = 1L;

            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}
