package com.back.domain.chat.chat.dto

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MessageDto 테스트")
internal class MessageDtoTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("생성자 테스트")
    internal inner class ConstructorTest {

        @Test
        @DisplayName("간단한 매개변수 생성자 테스트")
        fun simpleParameterConstructor() {
            // Given
            val senderName = "테스트유저"
            val content = "테스트 메시지"
            val senderId = 1L
            val chatRoomId = 2L

            // When
            val messageDto = MessageDto(senderName, content, senderId, chatRoomId)

            // Then
            assertThat(messageDto.senderName).isEqualTo(senderName)
            assertThat(messageDto.content).isEqualTo(content)
            assertThat(messageDto.senderId).isEqualTo(senderId)
            assertThat(messageDto.chatRoomId).isEqualTo(chatRoomId)
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    internal inner class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터를 가진 객체는 equals true")
        fun equals_SameData_True() {
            // Given
            val dto1 = MessageDto("테스트유저", "메시지", 1L, 2L).apply {
                senderEmail = "test@test.com"
            }
            val dto2 = MessageDto("테스트유저", "메시지", 1L, 2L).apply {
                senderEmail = "test@test.com"
            }

            // When & Then
            assertThat(dto1).isEqualTo(dto2)
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode())
        }

        @Test
        @DisplayName("다른 데이터를 가진 객체는 equals false")
        fun equals_DifferentData_False() {
            // Given
            val dto1 = MessageDto("테스트유저", "메시지1", 1L, 2L)
            val dto2 = MessageDto("테스트유저", "메시지2", 1L, 2L)

            // When & Then
            assertThat(dto1).isNotEqualTo(dto2)
        }
    }

    @Nested
    @DisplayName("JSON 직렬화/역직렬화 테스트")
    internal inner class JsonSerializationTest {

        @Test
        @DisplayName("JSON 직렬화 테스트")
        fun jsonSerialization() {
            // Given
            val messageDto = MessageDto("테스트유저", "테스트 메시지", 1L, 2L).apply {
                senderEmail = "test@test.com"
                messageType = "NORMAL"
            }

            // When
            val json = objectMapper.writeValueAsString(messageDto)

            // Then
            assertThat(json).contains("\"senderId\":1")
            assertThat(json).contains("\"chatRoomId\":2")
            assertThat(json).contains("\"senderName\":\"테스트유저\"")
            assertThat(json).contains("\"content\":\"테스트 메시지\"")
        }

        @Test
        @DisplayName("JSON 역직렬화 테스트")
        fun jsonDeserialization() {
            // Given
            val json = """
                {
                    "senderId": 1,
                    "chatRoomId": 2,
                    "senderName": "테스트유저",
                    "senderEmail": "test@test.com",
                    "content": "테스트 메시지",
                    "messageType": "NORMAL"
                }
            """.trimIndent()

            // When
            val messageDto = objectMapper.readValue(json, MessageDto::class.java)

            // Then
            assertThat(messageDto.senderId).isEqualTo(1L)
            assertThat(messageDto.chatRoomId).isEqualTo(2L)
            assertThat(messageDto.senderName).isEqualTo("테스트유저")
            assertThat(messageDto.content).isEqualTo("테스트 메시지")
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    internal inner class BusinessLogicTest {

        @Test
        @DisplayName("일반 사용자 메시지 생성")
        fun normalUserMessage() {
            // Given & When
            val messageDto = MessageDto("홍길동", "안녕하세요!", 1L, 10L).apply {
                senderEmail = "hong@test.com"
                messageType = "NORMAL"
            }

            // Then
            assertThat(messageDto.senderName).isEqualTo("홍길동")
            assertThat(messageDto.content).isEqualTo("안녕하세요!")
            assertThat(messageDto.senderId).isEqualTo(1L)
            assertThat(messageDto.chatRoomId).isEqualTo(10L)
            assertThat(messageDto.messageType).isEqualTo("NORMAL")
        }

        @Test
        @DisplayName("시스템 알림 메시지 생성")
        fun systemNotificationMessage() {
            // Given & When
            val messageDto = MessageDto().apply {
                setSender("System")
                content = "홍길동님이 채팅방을 나갔습니다."
                senderId = -1L
                chatRoomId = 10L
                messageType = "LEAVE_NOTIFICATION"
            }

            // Then
            assertThat(messageDto.senderName).isEqualTo("System")
            assertThat(messageDto.content).contains("나갔습니다")
            assertThat(messageDto.senderId).isEqualTo(-1L)
            assertThat(messageDto.messageType).isEqualTo("LEAVE_NOTIFICATION")
        }

        @Test
        @DisplayName("setSender 메서드 테스트")
        fun setSenderMethod() {
            // Given
            val messageDto = MessageDto()
            val sender = "테스트 발신자"

            // When
            messageDto.setSender(sender)

            // Then
            assertThat(messageDto.senderName).isEqualTo(sender)
        }
    }
}
