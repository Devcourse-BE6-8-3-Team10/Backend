package com.back.domain.chat.chat.service

import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.Message
import com.back.domain.chat.chat.entity.RoomParticipant
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.repository.MessageRepository
import com.back.domain.chat.chat.repository.RoomParticipantRepository
import com.back.domain.chat.redis.service.RedisMessageService
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.security.Principal
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ChatService 테스트")
internal class ChatServiceTest {

    @Mock
    private lateinit var messageRepository: MessageRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var chatRoomRepository: ChatRoomRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var roomParticipantRepository: RoomParticipantRepository

    @Mock
    private lateinit var redisMessageService: RedisMessageService

    @Mock
    private lateinit var principal: Principal

    @InjectMocks
    private lateinit var chatService: ChatService

    private lateinit var testUser: Member
    private lateinit var postAuthor: Member
    private lateinit var testPost: Post
    private lateinit var testChatRoom: ChatRoom
    private lateinit var testMessage: Message
    private lateinit var testParticipant: RoomParticipant

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = Member.builder()
            .email("test@test.com")
            .password("password")
            .name("테스트유저")
            .role(Role.USER)
            .status(Status.ACTIVE)
            .build()
            .also { ReflectionTestUtils.setField(it, "id", 1L) }

        // 게시글 작성자 생성
        postAuthor = Member.builder()
            .email("author@test.com")
            .password("password")
            .name("게시글작성자")
            .role(Role.USER)
            .status(Status.ACTIVE)
            .build()
            .also { ReflectionTestUtils.setField(it, "id", 2L) }

        // 테스트 게시글 생성
        testPost = Post.builder()
            .member(postAuthor)
            .title("테스트 게시글")
            .description("테스트 설명")
            .category(Post.Category.PRODUCT)
            .price(100000)
            .status(Post.Status.SALE)
            .build()
            .also { ReflectionTestUtils.setField(it, "id", 1L) }

        // 테스트 채팅방 생성
        testChatRoom = ChatRoom(testPost, testUser)
            .also { ReflectionTestUtils.setField(it, "id", 1L) }

        // 테스트 메시지 생성
        testMessage = Message().apply {
            updateMember(testUser)
            updateContent("첫번째 메시지")
            updateChatRoom(testChatRoom)
        }

        // 테스트 참여자 생성
        testParticipant = RoomParticipant(testChatRoom, testUser)
    }

    @Nested
    @DisplayName("메시지 저장 테스트")
    internal inner class SaveMessageTest {

        @Test
        @DisplayName("정상적인 메시지 저장")
        fun t1() {
            // Given
            val messageDto = MessageDto("테스트유저", "안녕하세요", 1L, 1L)
            given(memberRepository.findById(1L)).willReturn(Optional.of(testUser))
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom))
            given(messageRepository.save(any(Message::class.java))).willReturn(testMessage)

            // When
            val result = chatService.saveMessage(messageDto)

            // Then
            assertThat(result).isNotNull
            assertThat(result.content).isEqualTo("첫번째 메시지")
            assertThat(result.member).isEqualTo(testUser)
            assertThat(result.chatRoom).isEqualTo(testChatRoom)
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 메시지 저장 시 예외")
        fun t2() {
            // Given
            val messageDto = MessageDto("테스트유저", "안녕하세요", 999L, 1L)
            given(memberRepository.findById(999L)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { chatService.saveMessage(messageDto) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404-3 : 존재하지 않는 사용자입니다.")
        }

        @Test
        @DisplayName("존재하지 않는 채팅방으로 메시지 저장 시 예외")
        fun t3() {
            // Given
            val messageDto = MessageDto("테스트유저", "안녕하세요", 1L, 999L)
            given(memberRepository.findById(1L)).willReturn(Optional.of(testUser))
            given(chatRoomRepository.findById(999L)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { chatService.saveMessage(messageDto) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404-4 : 존재하지 않는 채팅방입니다.")
        }
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    internal inner class CreateChatRoomTest {

        @Test
        @DisplayName("로그인하지 않은 사용자의 채팅방 생성 시 예외")
        fun t4() {
            // Given
            val userEmail = ""
            val postId = 1L

            // When & Then
            assertThatThrownBy { chatService.createChatRoom(postId, userEmail) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("400-1 : 로그인 하셔야 합니다.")
        }

        @Test
        @DisplayName("존재하지 않는 게시글로 채팅방 생성 시 예외")
        fun t5() {
            // Given
            val userEmail = "test@test.com"
            val postId = 999L

            given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser))
            given(postRepository.findById(postId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { chatService.createChatRoom(postId, userEmail) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404-1 : 존재하지 않는 게시글입니다.")
        }
    }

    @Nested
    @DisplayName("채팅방 메시지 조회 테스트")
    internal inner class GetChatRoomMessagesTest {

        @Test
        @DisplayName("채팅방 메시지 조회 성공")
        fun t6() {
            // Given
            val chatRoomId = 1L
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(true)
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.id))
                .willReturn(true)

            val messages = listOf(testMessage)
            given(messageRepository.findByChatRoomId(chatRoomId)).willReturn(messages)

            // When
            val result = chatService.getChatRoomMessages(chatRoomId, principal)

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].content).isEqualTo("첫번째 메시지")
            assertThat(result[0].senderName).isEqualTo("테스트유저")
        }

        @Test
        @DisplayName("존재하지 않는 채팅방 조회 시 예외")
        fun t7() {
            // Given
            val chatRoomId = 999L
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(false)

            // When & Then
            assertThatThrownBy { chatService.getChatRoomMessages(chatRoomId, principal) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404-4 : 존재하지 않는 채팅방입니다.")
        }

        @Test
        @DisplayName("채팅방 참여자가 아닌 경우 예외")
        fun t8() {
            // Given
            val chatRoomId = 1L
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(true)
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.id))
                .willReturn(false)

            // When & Then
            assertThatThrownBy { chatService.getChatRoomMessages(chatRoomId, principal) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403-1 : 채팅방 참여자만 메시지를 조회할 수 있습니다.")
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회 테스트")
    internal inner class GetMyChatRoomsTest {

        @Test
        @DisplayName("내 채팅방 목록 조회 성공")
        fun t9() {
            // Given
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))

            val participations = listOf(testParticipant)
            given(roomParticipantRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(testUser.id))
                .willReturn(participations)
            given(messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(testChatRoom.id))
                .willReturn(testMessage)

            // When
            val result = chatService.getMyChatRooms(principal)

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo(testChatRoom.roomName)
            assertThat(result[0].lastContent).isEqualTo("첫번째 메시지")
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 목록 조회 시 예외")
        fun t10() {
            // Given
            given(principal.name).willReturn("")

            // When & Then
            assertThatThrownBy { chatService.getMyChatRooms(principal) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("400-1 : 로그인 하셔야 합니다.")
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 테스트")
    internal inner class LeaveChatRoomTest {

        @Test
        @DisplayName("채팅방 나가기 성공")
        fun t11() {
            // Given
            val chatRoomId = 1L
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))
            given(roomParticipantRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.id))
                .willReturn(Optional.of(testParticipant))
            given(roomParticipantRepository.existsByChatRoomIdAndIsActiveTrue(chatRoomId)).willReturn(true)

            // When & Then
            assertThatCode { chatService.leaveChatRoom(chatRoomId, principal) }
                .doesNotThrowAnyException()

            verify(roomParticipantRepository).save(testParticipant)
            verify(redisMessageService).publishMessage(any(MessageDto::class.java))
            assertThat(testParticipant.isActive()).isFalse
            assertThat(testParticipant.leftAt).isNotNull
        }

        @Test
        @DisplayName("참여하지 않은 채팅방 나가기 시 예외")
        fun t12() {
            // Given
            val chatRoomId = 1L
            given(principal.name).willReturn("test@test.com")
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser))
            given(roomParticipantRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.id))
                .willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { chatService.leaveChatRoom(chatRoomId, principal) }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404-5 : 채팅방 참여자가 아닙니다.")
        }
    }

    @Nested
    @DisplayName("참여자 확인 테스트")
    internal inner class IsParticipantTest {

        @Test
        @DisplayName("참여자인 경우 true 반환")
        fun t13() {
            // Given
            val chatRoomId = 1L
            val memberId = 1L
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId))
                .willReturn(true)

            // When
            val result = chatService.isParticipant(chatRoomId, memberId)

            // Then
            assertThat(result).isTrue
        }

        @Test
        @DisplayName("참여자가 아닌 경우 false 반환")
        fun t14() {
            // Given
            val chatRoomId = 1L
            val memberId = 1L
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId))
                .willReturn(false)

            // When
            val result = chatService.isParticipant(chatRoomId, memberId)

            // Then
            assertThat(result).isFalse
        }
    }
}
