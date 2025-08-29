package com.back.domain.chat.chat.entity;

import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.entity.Status;
import com.back.domain.post.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Chat 엔티티 테스트")
class ChatEntityTest {

    private Member testUser;
    private Member postAuthor;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Member.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트유저")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();

        // 게시글 작성자 생성
        postAuthor = Member.builder()
                .email("author@test.com")
                .password("password")
                .name("게시글작성자")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();

        // 테스트 게시글 생성
        testPost = Post.builder()
                .member(postAuthor)
                .title("테스트 게시글")
                .description("테스트 설명")
                .category(Post.Category.PRODUCT)
                .price(100000)
                .status(Post.Status.SALE)
                .build();
    }

    @Nested
    @DisplayName("ChatRoom 엔티티 테스트")
    class ChatRoomTest {

        @Test
        @DisplayName("ChatRoom 기본 생성자 테스트")
        void chatRoom_DefaultConstructor() {
            // Given & When
            ChatRoom chatRoom = new ChatRoom();

            // Then
            assertThat(chatRoom).isNotNull();
            assertThat(chatRoom.getPost()).isNull();
            assertThat(chatRoom.getMember()).isNull();
            assertThat(chatRoom.getRoomName()).isNull();
        }

        @Test
        @DisplayName("ChatRoom 자동 이름 생성 생성자 테스트")
        void chatRoom_AutoNameConstructor() {
            // Given & When
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);

            // Then
            assertThat(chatRoom.getPost()).isEqualTo(testPost);
            assertThat(chatRoom.getMember()).isEqualTo(testUser);
            assertThat(chatRoom.getRoomName()).isEqualTo("테스트 게시글 - 테스트유저");
        }

        @Test
        @DisplayName("ChatRoom 사용자 정의 이름 생성자 테스트")
        void chatRoom_CustomNameConstructor() {
            // Given
            String customName = "사용자 정의 채팅방 이름";

            // When
            ChatRoom chatRoom = new ChatRoom(testPost, testUser, customName);

            // Then
            assertThat(chatRoom.getPost()).isEqualTo(testPost);
            assertThat(chatRoom.getMember()).isEqualTo(testUser);
            assertThat(chatRoom.getRoomName()).isEqualTo(customName);
        }

        @Test
        @DisplayName("ChatRoom Getter/Setter 테스트")
        void chatRoom_GetterSetter() {
            // Given
            ChatRoom chatRoom = new ChatRoom();

            // When
            chatRoom.updatePost(testPost);
            chatRoom.updateMember(testUser);
            chatRoom.updateRoomName("테스트 채팅방");

            // Then
            assertThat(chatRoom.getPost()).isEqualTo(testPost);
            assertThat(chatRoom.getMember()).isEqualTo(testUser);
            assertThat(chatRoom.getRoomName()).isEqualTo("테스트 채팅방");
        }
    }

    @Nested
    @DisplayName("Message 엔티티 테스트")
    class MessageTest {

        @Test
        @DisplayName("Message 기본 생성자 테스트")
        void message_DefaultConstructor() {
            // Given & When
            Message message = new Message();

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getSender()).isNull();
            assertThat(message.getContent()).isNull();
            assertThat(message.getChatRoom()).isNull();
        }

        @Test
        @DisplayName("Message 기본 생성자 (Member, Content) 테스트")
        void message_BasicConstructor() {
            // Given
            String content = "테스트 메시지";

            // When
            Message message = new Message(testUser, content);

            // Then
            assertThat(message.getSender()).isEqualTo(testUser);
            assertThat(message.getContent()).isEqualTo(content);
            assertThat(message.getChatRoom()).isNull();
        }

        @Test
        @DisplayName("Message DTO 기반 생성자 테스트")
        void message_DtoConstructor() {
            // Given
            String content = "DTO 테스트 메시지";
             MessageDto messageDto = new MessageDto("테스트유저", content, 1L, 1L);

            // When
            Message message = new Message(messageDto, testUser);

            // Then
            assertThat(message.getSender()).isEqualTo(testUser);
            assertThat(message.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("Message Getter/Setter 테스트")
        void message_GetterSetter() {
            // Given
            Message message = new Message();
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);

            // When
            message.setSender(testUser);
            message.setContent("설정된 메시지");
            message.setChatRoom(chatRoom);

            // Then
            assertThat(message.getSender()).isEqualTo(testUser);
            assertThat(message.getContent()).isEqualTo("설정된 메시지");
            assertThat(message.getChatRoom()).isEqualTo(chatRoom);
        }
    }

    @Nested
    @DisplayName("RoomParticipant 엔티티 테스트")
    class RoomParticipantTest {

        @Test
        @DisplayName("RoomParticipant 기본 생성자 테스트")
        void roomParticipant_DefaultConstructor() {
            // Given & When
            RoomParticipant participant = new RoomParticipant();

            // Then
            assertThat(participant).isNotNull();
            assertThat(participant.getChatRoom()).isNull();
            assertThat(participant.getMember()).isNull();
            assertThat(participant.getLeftAt()).isNull();
        }

        @Test
        @DisplayName("RoomParticipant 매개변수 생성자 테스트")
        void roomParticipant_ParameterConstructor() {
            // Given
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);

            // When
            RoomParticipant participant = new RoomParticipant(chatRoom, testUser);

            // Then
            assertThat(participant.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant.getMember()).isEqualTo(testUser);
            assertThat(participant.isActive()).isTrue();
            assertThat(participant.getLeftAt()).isNull();
        }

        @Test
        @DisplayName("RoomParticipant Getter/Setter 테스트")
        void roomParticipant_GetterSetter() {
            // Given
            RoomParticipant participant = new RoomParticipant();
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);
            LocalDateTime leftTime = LocalDateTime.now();

            // When
            participant.setChatRoom(chatRoom);
            participant.setMember(testUser);
            participant.setActive(false);
            participant.setLeftAt(leftTime);

            // Then
            assertThat(participant.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant.getMember()).isEqualTo(testUser);
            assertThat(participant.isActive()).isFalse();
            assertThat(participant.getLeftAt()).isEqualTo(leftTime);
        }

        @Test
        @DisplayName("RoomParticipant 활성 상태 변경 테스트")
        void roomParticipant_ActiveStatusChange() {
            // Given
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);
            RoomParticipant participant = new RoomParticipant(chatRoom, testUser);
            LocalDateTime leftTime = LocalDateTime.now();

            // When - 비활성화
            participant.setActive(false);
            participant.setLeftAt(leftTime);

            // Then
            assertThat(participant.isActive()).isFalse();
            assertThat(participant.getLeftAt()).isEqualTo(leftTime);

            // When - 다시 활성화
            participant.setActive(true);
            participant.setLeftAt(null);

            // Then
            assertThat(participant.isActive()).isTrue();
            assertThat(participant.getLeftAt()).isNull();
        }
    }

    @Nested
    @DisplayName("엔티티 간 연관관계 테스트")
    class EntityRelationshipTest {

        @Test
        @DisplayName("ChatRoom과 Message 연관관계 테스트")
        void chatRoom_Message_Relationship() {
            // Given
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);
            Message message = new Message(testUser, "연관관계 테스트");

            // When
            message.setChatRoom(chatRoom);

            // Then
            assertThat(message.getChatRoom()).isEqualTo(chatRoom);
            assertThat(message.getSender()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("ChatRoom과 RoomParticipant 연관관계 테스트")
        void chatRoom_RoomParticipant_Relationship() {
            // Given
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);

            // When
            RoomParticipant participant1 = new RoomParticipant(chatRoom, testUser);
            RoomParticipant participant2 = new RoomParticipant(chatRoom, postAuthor);

            // Then
            assertThat(participant1.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant1.getMember()).isEqualTo(testUser);
            assertThat(participant2.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant2.getMember()).isEqualTo(postAuthor);
        }

        @Test
        @DisplayName("Post와 ChatRoom 연관관계 테스트")
        void post_ChatRoom_Relationship() {
            // Given & When
            ChatRoom chatRoom1 = new ChatRoom(testPost, testUser);
            ChatRoom chatRoom2 = new ChatRoom(testPost, postAuthor);

            // Then
            assertThat(chatRoom1.getPost()).isEqualTo(testPost);
            assertThat(chatRoom2.getPost()).isEqualTo(testPost);
            assertThat(chatRoom1.getMember()).isEqualTo(testUser);
            assertThat(chatRoom2.getMember()).isEqualTo(postAuthor);
        }

        @Test
        @DisplayName("Member와 ChatRoom, Message, RoomParticipant 연관관계 테스트")
        void member_AllEntities_Relationship() {
            // Given
            ChatRoom chatRoom = new ChatRoom(testPost, testUser);
            Message message = new Message(testUser, "종합 연관관계 테스트");
            RoomParticipant participant = new RoomParticipant(chatRoom, testUser);

            // When
            message.setChatRoom(chatRoom);

            // Then
            assertThat(chatRoom.getMember()).isEqualTo(testUser);
            assertThat(message.getSender()).isEqualTo(testUser);
            assertThat(participant.getMember()).isEqualTo(testUser);

            assertThat(message.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant.getChatRoom()).isEqualTo(chatRoom);
        }
    }
}
