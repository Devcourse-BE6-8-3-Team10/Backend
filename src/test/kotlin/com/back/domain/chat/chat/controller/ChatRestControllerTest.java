package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.service.ChatService;
import com.back.domain.files.files.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ChatRestController 통합 테스트")
class ChatRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatService chatService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private Storage storage;

    @Nested
    @DisplayName("채팅방 메시지 조회 API 테스트")
    class GetChatRoomMessagesTest {

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("채팅방 메시지 조회 성공")
        void getChatRoomMessages_Success() throws Exception {
            // Given
            Long postId = 1L;
            String userEmail = "user2@user.com";
            Long chatRoomId = chatService.createChatRoom(postId, userEmail);

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("채팅방 메시지 조회 성공"))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("존재하지 않는 채팅방 조회 시 404 에러")
        void getChatRoomMessages_NotFound() throws Exception {
            // Given
            Long chatRoomId = 999L;

            // When & Then
            mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("권한이 없는 채팅방 조회 시 403 에러")
        void getChatRoomMessages_Forbidden() throws Exception {
            // Given
            Long postId = 1L;
            String userEmail = "user1@user.com";
            Long chatRoomId = chatService.createChatRoom(postId, userEmail);

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
        @WithUserDetails("user2@user.com")
        @DisplayName("채팅방 생성 성공")
        void createChatRoom_Success() throws Exception {
            // Given
            Long postId = 1L;

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("채팅방 생성 성공"))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("존재하지 않는 게시글로 채팅방 생성 시 404 에러")
        void createChatRoom_PostNotFound() throws Exception {
            // Given
            Long postId = 999L;

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 생성 시 403 에러")
        void createChatRoom_Unauthorized() throws Exception {
            // Given
            Long postId = 1L;

            // When & Then
            mockMvc.perform(post("/api/chat/rooms/{postId}", postId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회 API 테스트")
    class GetMyChatRoomsTest {

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("내 채팅방 목록 조회 성공")
        void getMyChatRooms_Success() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/chat/rooms/my")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("내 채팅방 목록 조회 성공"))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 목록 조회 시 403 에러")
        void getMyChatRooms_Unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/chat/rooms/my")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 API 테스트")
    class LeaveChatRoomTest {

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("채팅방 나가기 성공")
        void leaveChatRoom_Success() throws Exception {
            // Given
            Long postId = 1L;
            String userEmail = "user2@user.com";
            Long chatRoomId = chatService.createChatRoom(postId, userEmail);



            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("채팅방 나가기 성공"));
        }

        @Test
        @WithUserDetails("user2@user.com")
        @DisplayName("참여하지 않은 채팅방 나가기 시 404 에러")
        void leaveChatRoom_NotParticipant() throws Exception {
            // Given
            Long chatRoomId = 999L;

            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 나가기 시 403 에러")
        void leaveChatRoom_Unauthorized() throws Exception {
            // Given
            Long chatRoomId = 1L;

            // When & Then
            mockMvc.perform(delete("/api/chat/rooms/{chatRoomId}", chatRoomId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
