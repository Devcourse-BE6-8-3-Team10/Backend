package com.back.domain.chat.chat.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageDto 테스트")
class MessageDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {


        @Test
        @DisplayName("간단한 매개변수 생성자 테스트")
        void simpleParameterConstructor() {
            // Given
            String senderName = "테스트유저";
            String content = "테스트 메시지";
            Long senderId = 1L;
            Long chatRoomId = 2L;

            // When
            MessageDto messageDto = new MessageDto(senderName, content, senderId, chatRoomId);

            // Then
            assertThat(messageDto.getSenderName()).isEqualTo(senderName);
            assertThat(messageDto.getContent()).isEqualTo(content);
            assertThat(messageDto.getSenderId()).isEqualTo(senderId);
            assertThat(messageDto.getChatRoomId()).isEqualTo(chatRoomId);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터를 가진 객체는 equals true")
        void equals_SameData_True() {
            // Given
            MessageDto dto1 = new MessageDto("테스트유저", "메시지", 1L, 2L);
            dto1.setSenderEmail("test@test.com");
            MessageDto dto2 = new MessageDto("테스트유저", "메시지", 1L, 2L);
            dto2.setSenderEmail("test@test.com");

            // When & Then
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터를 가진 객체는 equals false")
        void equals_DifferentData_False() {
            // Given
            MessageDto dto1 = new MessageDto("테스트유저", "메시지1", 1L, 2L);
            MessageDto dto2 = new MessageDto("테스트유저", "메시지2", 1L, 2L);

            // When & Then
            assertThat(dto1).isNotEqualTo(dto2);
        }
    }

    @Nested
    @DisplayName("JSON 직렬화/역직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("JSON 직렬화 테스트")
        void jsonSerialization() throws Exception {
            // Given
            MessageDto messageDto = new MessageDto("테스트유저", "테스트 메시지", 1L, 2L);
            messageDto.setSenderEmail("test@test.com");
            messageDto.setMessageType("NORMAL");

            // When
            String json = objectMapper.writeValueAsString(messageDto);

            // Then
            assertThat(json).contains("\"senderId\":1");
            assertThat(json).contains("\"chatRoomId\":2");
            assertThat(json).contains("\"senderName\":\"테스트유저\"");
            assertThat(json).contains("\"content\":\"테스트 메시지\"");
        }

        @Test
        @DisplayName("JSON 역직렬화 테스트")
        void jsonDeserialization() throws Exception {
            // Given
            String json = """
                {
                    "senderId": 1,
                    "chatRoomId": 2,
                    "senderName": "테스트유저",
                    "senderEmail": "test@test.com",
                    "content": "테스트 메시지",
                    "messageType": "NORMAL"
                }
                """;

            // When
            MessageDto messageDto = objectMapper.readValue(json, MessageDto.class);

            // Then
            assertThat(messageDto.getSenderId()).isEqualTo(1L);
            assertThat(messageDto.getChatRoomId()).isEqualTo(2L);
            assertThat(messageDto.getSenderName()).isEqualTo("테스트유저");
            assertThat(messageDto.getContent()).isEqualTo("테스트 메시지");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("일반 사용자 메시지 생성")
        void normalUserMessage() {
            // Given & When
            MessageDto messageDto = new MessageDto("홍길동", "안녕하세요!", 1L, 10L);
            messageDto.setSenderEmail("hong@test.com");
            messageDto.setMessageType("NORMAL");

            // Then
            assertThat(messageDto.getSenderName()).isEqualTo("홍길동");
            assertThat(messageDto.getContent()).isEqualTo("안녕하세요!");
            assertThat(messageDto.getSenderId()).isEqualTo(1L);
            assertThat(messageDto.getChatRoomId()).isEqualTo(10L);
            assertThat(messageDto.getMessageType()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("시스템 알림 메시지 생성")
        void systemNotificationMessage() {
            // Given & When
            MessageDto messageDto = new MessageDto();
            messageDto.setSender("System");
            messageDto.setContent("홍길동님이 채팅방을 나갔습니다.");
            messageDto.setSenderId(-1L);
            messageDto.setChatRoomId(10L);
            messageDto.setMessageType("LEAVE_NOTIFICATION");

            // Then
            assertThat(messageDto.getSenderName()).isEqualTo("System");
            assertThat(messageDto.getContent()).contains("나갔습니다");
            assertThat(messageDto.getSenderId()).isEqualTo(-1L);
            assertThat(messageDto.getMessageType()).isEqualTo("LEAVE_NOTIFICATION");
        }

        @Test
        @DisplayName("setSender 메서드 테스트")
        void setSenderMethod() {
            // Given
            MessageDto messageDto = new MessageDto();
            String sender = "테스트 발신자";

            // When
            messageDto.setSender(sender);

            // Then
            assertThat(messageDto.getSenderName()).isEqualTo(sender);
        }
    }
}
