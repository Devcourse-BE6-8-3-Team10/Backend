package com.back.domain.chat.chat.entity

import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Chat 엔티티 테스트")
internal class ChatEntityTest {

    private lateinit var testUser: Member
    private lateinit var postAuthor: Member
    private lateinit var testPost: Post

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = Member(
            "test@test.com",
            "password",
            "테스트유저",
            null,
            Role.USER,
            Status.ACTIVE
        )

        // 게시글 작성자 생성
        postAuthor = Member(
            "author@test.com",
            "password",
            "작성자",
            null,
            Role.USER,
            Status.ACTIVE
        )

        // 테스트 게시글 생성
        testPost = Post(
            postAuthor,
            "테스트 게시글",
            "테스트 설명",
            Post.Category.PRODUCT,
            100000,
            Post.Status.SALE
//            0,
//            mutableListOf(),
//            null,
//            mutableListOf(),
//            mutableListOf()
        )
    }

    @Nested
    @DisplayName("ChatRoom 엔티티 테스트")
    internal inner class ChatRoomTest {

        @Test
        @DisplayName("ChatRoom 기본 생성자 테스트")
        fun t1() {
            // Given & When
            val chatRoom = ChatRoom()

            // Then
            assertThat(chatRoom).isNotNull
            assertThat(chatRoom.post).isNull()
            assertThat(chatRoom.member).isNull()
            assertThat(chatRoom.roomName).isNull()
        }

        @Test
        @DisplayName("ChatRoom 자동 이름 생성 생성자 테스트")
        fun t2() {
            // Given & When
            val chatRoom = ChatRoom(testPost, testUser)

            // Then
            assertThat(chatRoom.post).isEqualTo(testPost)
            assertThat(chatRoom.member).isEqualTo(testUser)
            assertThat(chatRoom.roomName).isEqualTo("테스트 게시글 - 테스트유저")
        }

        @Test
        @DisplayName("ChatRoom 사용자 정의 이름 생성자 테스트")
        fun t3() {
            // Given
            val customName = "사용자 정의 채팅방 이름"

            // When
            val chatRoom = ChatRoom(testPost, testUser, customName)

            // Then
            assertThat(chatRoom.post).isEqualTo(testPost)
            assertThat(chatRoom.member).isEqualTo(testUser)
            assertThat(chatRoom.roomName).isEqualTo(customName)
        }

        @Test
        @DisplayName("ChatRoom Getter/Setter 테스트")
        fun t4() {
            // Given
            val chatRoom = ChatRoom()

            // When
            chatRoom.apply {
                updatePost(testPost)
                updateMember(testUser)
                updateRoomName("테스트 채팅방")
            }

            // Then
            assertThat(chatRoom.post).isEqualTo(testPost)
            assertThat(chatRoom.member).isEqualTo(testUser)
            assertThat(chatRoom.roomName).isEqualTo("테스트 채팅방")
        }
    }

    @Nested
    @DisplayName("Message 엔티티 테스트")
    internal inner class MessageTest {

        @Test
        @DisplayName("Message 기본 생성자 테스트")
        fun t5() {
            // Given & When
            val message = Message()

            // Then
            assertThat(message).isNotNull
            assertThat(message.member).isNull()
            assertThat(message.content).isEmpty()
            assertThat(message.chatRoom).isNull()
        }

        @Test
        @DisplayName("Message DTO 기반 생성자 테스트")
        fun t6() {
            // Given
            val content = "DTO 테스트 메시지"
            val messageDto = MessageDto("테스트유저", content, 1L, 1L)

            // When
            val message = Message(messageDto, testUser)

            // Then
            assertThat(message.member).isEqualTo(testUser)
            assertThat(message.content).isEqualTo(content)
        }

        @Test
        @DisplayName("Message 업데이트 메서드 테스트")
        fun t7() {
            // Given
            val message = Message()
            val chatRoom = ChatRoom(testPost, testUser)

            // When
            message.apply {
                updateMember(testUser)
                updateContent("설정된 메시지")
                updateChatRoom(chatRoom)
            }

            // Then
            assertThat(message.member).isEqualTo(testUser)
            assertThat(message.content).isEqualTo("설정된 메시지")
            assertThat(message.chatRoom).isEqualTo(chatRoom)
        }
    }

    @Nested
    @DisplayName("RoomParticipant 엔티티 테스트")
    internal inner class RoomParticipantTest {

        @Test
        @DisplayName("RoomParticipant 생성자 테스트")
        fun t8() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)

            // When
            val participant = RoomParticipant(chatRoom, testUser)

            // Then
            assertThat(participant).isNotNull
            assertThat(participant.chatRoom).isEqualTo(chatRoom)
            assertThat(participant.member).isEqualTo(testUser)
            assertThat(participant.leftAt).isNull()
            assertThat(participant.active).isTrue
        }

        @Test
        @DisplayName("RoomParticipant 매개변수 생성자 테스트")
        fun t9() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)

            // When
            val participant = RoomParticipant(chatRoom, testUser)

            // Then
            assertThat(participant.chatRoom).isEqualTo(chatRoom)
            assertThat(participant.member).isEqualTo(testUser)
            assertThat(participant.active).isTrue
            assertThat(participant.leftAt).isNull()
        }

        @Test
        @DisplayName("RoomParticipant leave/activate 메서드 테스트")
        fun t10() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)
            val participant = RoomParticipant(chatRoom, testUser)

            // When - 채팅방 나가기
            participant.leave()

            // Then
            assertThat(participant.active).isFalse
            assertThat(participant.leftAt).isNotNull

            // When - 채팅방 재활성화
            participant.activate()

            // Then
            assertThat(participant.active).isTrue
            assertThat(participant.leftAt).isNull()
        }

        @Test
        @DisplayName("RoomParticipant 상태 변경 시나리오 테스트")
        fun t11() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)
            val participant = RoomParticipant(chatRoom, testUser)

            // 초기 상태 확인
            assertThat(participant.active).isTrue
            assertThat(participant.leftAt).isNull()

            // When - 채팅방 나가기
            participant.leave()

            // Then
            assertThat(participant.active).isFalse
            assertThat(participant.leftAt).isNotNull

            // When - 재참여
            participant.rejoin()

            // Then
            assertThat(participant.active).isTrue
            assertThat(participant.leftAt).isNull()
        }
    }

    @Nested
    @DisplayName("엔티티 간 연관관계 테스트")
    internal inner class EntityRelationshipTest {

        @Test
        @DisplayName("ChatRoom과 Message 연관관계 테스트")
        fun t12() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)
            val message = Message().apply {
                updateMember(testUser)
                updateContent("연관관계 테스트")
            }

            // When
            message.updateChatRoom(chatRoom)

            // Then
            assertThat(message.chatRoom).isEqualTo(chatRoom)
            assertThat(message.member).isEqualTo(testUser)
        }

        @Test
        @DisplayName("ChatRoom과 RoomParticipant 연관관계 테스트")
        fun t13() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)

            // When
            val participant1 = RoomParticipant(chatRoom, testUser)
            val participant2 = RoomParticipant(chatRoom, postAuthor)

            // Then
            assertThat(participant1.chatRoom).isEqualTo(chatRoom)
            assertThat(participant1.member).isEqualTo(testUser)
            assertThat(participant2.chatRoom).isEqualTo(chatRoom)
            assertThat(participant2.member).isEqualTo(postAuthor)
        }

        @Test
        @DisplayName("Post와 ChatRoom 연관관계 테스트")
        fun t14() {
            // Given & When
            val chatRoom1 = ChatRoom(testPost, testUser)
            val chatRoom2 = ChatRoom(testPost, postAuthor)

            // Then
            assertThat(chatRoom1.post).isEqualTo(testPost)
            assertThat(chatRoom2.post).isEqualTo(testPost)
            assertThat(chatRoom1.member).isEqualTo(testUser)
            assertThat(chatRoom2.member).isEqualTo(postAuthor)
        }

        @Test
        @DisplayName("Member와 ChatRoom, Message, RoomParticipant 연관관계 테스트")
        fun t15() {
            // Given
            val chatRoom = ChatRoom(testPost, testUser)
            val message = Message().apply {
                updateMember(testUser)
                updateContent("종합 연관관계 테스트")
                updateChatRoom(chatRoom)
            }
            val participant = RoomParticipant(chatRoom, testUser)

            // Then
            assertThat(chatRoom.member).isEqualTo(testUser)
            assertThat(message.member).isEqualTo(testUser)
            assertThat(participant.member).isEqualTo(testUser)

            assertThat(message.chatRoom).isEqualTo(chatRoom)
            assertThat(participant.chatRoom).isEqualTo(chatRoom)
        }
    }
}
