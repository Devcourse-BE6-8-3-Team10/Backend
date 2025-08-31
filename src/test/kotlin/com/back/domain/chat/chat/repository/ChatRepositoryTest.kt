package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.Message
import com.back.domain.chat.chat.entity.RoomParticipant
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Chat Repository 테스트")
internal class ChatRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var chatRoomRepository: ChatRoomRepository

    @Autowired
    private lateinit var messageRepository: MessageRepository

    @Autowired
    private lateinit var roomParticipantRepository: RoomParticipantRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    private lateinit var testUser: Member
    private lateinit var postAuthor: Member
    private lateinit var testPost: Post
    private lateinit var testChatRoom: ChatRoom

    @BeforeEach
    fun setUp() {
        // 테스트 사용자들 생성 및 저장
        testUser = memberRepository.save(
            Member.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트유저")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build()
        )

        postAuthor = memberRepository.save(
            Member.builder()
                .email("author@test.com")
                .password("password")
                .name("게시글작성자")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build()
        )

        // 테스트 게시글 생성 및 저장
        testPost = postRepository.save(
            Post.builder()
                .member(postAuthor)
                .title("테스트 게시글")
                .description("테스트 설명")
                .category(Post.Category.PRODUCT)
                .price(100000)
                .status(Post.Status.SALE)
                .build()
        )

        // 테스트 채팅방 생성 및 저장
        testChatRoom = chatRoomRepository.save(ChatRoom(testPost, testUser))

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("ChatRoomRepository 테스트")
    internal inner class ChatRoomRepositoryTest {

        @Test
        @DisplayName("방 이름으로 채팅방 조회")
        fun findByRoomName() {
            // When
            val foundRoom = chatRoomRepository.findByRoomName(testChatRoom.roomName!!)

            // Then
            assertThat(foundRoom).isNotNull
            assertThat(foundRoom!!.roomName).isEqualTo(testChatRoom.roomName)
            assertThat(foundRoom.post!!.id).isEqualTo(testPost.id)
            assertThat(foundRoom.member!!.id).isEqualTo(testUser.id)
        }

        @Test
        @DisplayName("게시글 ID로 채팅방 목록 조회")
        fun findByPostId() {
            // Given - 같은 게시글에 대한 다른 채팅방 생성
            val anotherChatRoom = chatRoomRepository.save(ChatRoom(testPost, postAuthor))

            // When
            val chatRooms = chatRoomRepository.findByPostId(testPost.id)

            // Then
            assertThat(chatRooms).hasSize(2)
            assertThat(chatRooms).extracting("post.id")
                .containsOnly(testPost.id)
        }

        @Test
        @DisplayName("게시글 ID와 사용자 ID로 채팅방 조회")
        fun findByPostIdAndMemberId() {
            // When
            val foundRoom = chatRoomRepository.findByPostIdAndMemberId(testPost.id, testUser.id)

            // Then
            assertThat(foundRoom).isPresent
            assertThat(foundRoom.get().post!!.id).isEqualTo(testPost.id)
            assertThat(foundRoom.get().member!!.id).isEqualTo(testUser.id)
        }
    }

    @Nested
    @DisplayName("MessageRepository 테스트")
    internal inner class MessageRepositoryTest {

        @Test
        @DisplayName("채팅방 ID로 메시지 목록 조회")
        fun findByChatRoomId() {
            // Given
            val message1 = messageRepository.save(
                Message().apply {
                    updateMember(testUser)
                    updateContent("첫번째 메시지")
                    updateChatRoom(testChatRoom)
                }
            )

            val message2 = messageRepository.save(
                Message().apply {
                    updateMember(testUser)
                    updateContent("두번째 메시지")
                    updateChatRoom(testChatRoom)
                }
            )

            // When
            val messages = messageRepository.findByChatRoomId(testChatRoom.id)

            // Then
            assertThat(messages).hasSize(2)
            assertThat(messages).extracting("chatRoom.id")
                .containsOnly(testChatRoom.id)
            assertThat(messages).extracting("content")
                .containsExactlyInAnyOrder("첫번째 메시지", "두번째 메시지")
        }

        @Test
        @DisplayName("채팅방의 마지막 메시지 조회")
        fun findFirstByChatRoomIdOrderByCreatedAtDesc() {
            // Given
            messageRepository.save(
                Message().apply {
                    updateMember(testUser)
                    updateContent("첫번째 메시지")
                    updateChatRoom(testChatRoom)
                }
            )

            entityManager.flush()

            messageRepository.save(
                Message().apply {
                    updateMember(testUser)
                    updateContent("마지막 메시지")
                    updateChatRoom(testChatRoom)
                }
            )

            // When
            val foundMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(testChatRoom.id)

            // Then
            assertThat(foundMessage).isNotNull
            assertThat(foundMessage!!.content).isEqualTo("마지막 메시지")
        }
    }

    @Nested
    @DisplayName("RoomParticipantRepository 테스트")
    internal inner class RoomParticipantRepositoryTest {

        private lateinit var testParticipant: RoomParticipant
        private lateinit var authorParticipant: RoomParticipant

        @BeforeEach
        fun setUpParticipants() {
            testParticipant = roomParticipantRepository.save(
                RoomParticipant(testChatRoom, testUser)
            )

            authorParticipant = roomParticipantRepository.save(
                RoomParticipant(testChatRoom, postAuthor)
            )
        }

        @Test
        @DisplayName("채팅방과 사용자로 활성 참여자 존재 확인")
        fun existsByChatRoomIdAndMemberIdAndIsActiveTrue() {
            // When
            val exists = roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(
                testChatRoom.id, testUser.id
            )

            // Then
            assertThat(exists).isTrue
        }

        @Test
        @DisplayName("채팅방의 활성 참여자 목록 조회")
        fun findByChatRoomIdAndIsActiveTrue() {
            // When
            val participants = roomParticipantRepository.findByChatRoomIdAndIsActiveTrue(testChatRoom.id)

            // Then
            assertThat(participants).hasSize(2)
            assertThat(participants).extracting("member.id")
                .containsExactlyInAnyOrder(testUser.id, postAuthor.id)
        }
    }
}
