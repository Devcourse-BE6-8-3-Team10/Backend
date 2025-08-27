package com.back.domain.chat.chat.service;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.entity.Message;
import com.back.domain.chat.chat.entity.RoomParticipant;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.chat.chat.repository.MessageRepository;
import com.back.domain.chat.chat.repository.RoomParticipantRepository;
import com.back.domain.chat.redis.service.RedisMessageService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.entity.Status;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private RedisMessageService redisMessageService;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatService chatService;

    private Member testUser;
    private Member postAuthor;
    private Post testPost;
    private ChatRoom testChatRoom;
    private Message testMessage;
    private RoomParticipant testParticipant;

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

        // 테스트 채팅방 생성
        testChatRoom = new ChatRoom(testPost, testUser);

        // 테스트 메시지 생성
        testMessage = new Message(testUser, "테스트 메시지");
        testMessage.setChatRoom(testChatRoom);

        // 테스트 참여자 생성
        testParticipant = new RoomParticipant(testChatRoom, testUser);
    }

    @Nested
    @DisplayName("메시지 저장 테스트")
    class SaveMessageTest {

        @Test
        @DisplayName("정상적인 메시지 저장")
        void saveMessage_Success() {
            // Given
            MessageDto messageDto = new MessageDto("테스트유저", "안녕하세요", 1L, 1L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(messageRepository.save(any(Message.class))).willReturn(testMessage);

            // When
            Message result = chatService.saveMessage(messageDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("테스트 메시지");
            assertThat(result.getSender()).isEqualTo(testUser);
            assertThat(result.getChatRoom()).isEqualTo(testChatRoom);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 메시지 저장 시 예외")
        void saveMessage_UserNotFound_ThrowsException() {
            // Given
            MessageDto messageDto = new MessageDto("테스트유저", "안녕하세요", 999L, 1L);

            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.saveMessage(messageDto))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 채팅방으로 메시지 저장 시 예외")
        void saveMessage_ChatRoomNotFound_ThrowsException() {
            // Given
            MessageDto messageDto = new MessageDto("테스트유저", "안녕하세요", 1L, 999L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.saveMessage(messageDto))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("존재하지 않는 채팅방입니다.");
        }
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTest {

        @Test
        @DisplayName("새로운 채팅방 생성 성공")
        void createChatRoom_NewRoom_Success() {
            // Given
            String userEmail = "test@test.com";
            Long postId = 1L;

            given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(postRepository.findById(postId)).willReturn(Optional.of(testPost));
            given(chatRoomRepository.findByPostId(postId)).willReturn(Arrays.asList());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(testChatRoom);
            given(roomParticipantRepository.save(any(RoomParticipant.class))).willReturn(testParticipant);

            // When
            Long result = chatService.createChatRoom(postId, userEmail);

            // Then
            assertThat(result).isNotNull();
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(roomParticipantRepository, times(2)).save(any(RoomParticipant.class));
        }

        @Test
        @DisplayName("로그인하지 않은 사용자의 채팅방 생성 시 예외")
        void createChatRoom_NotLoggedIn_ThrowsException() {
            // Given
            String userEmail = "";
            Long postId = 1L;

            // When & Then
            assertThatThrownBy(() -> chatService.createChatRoom(postId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("로그인 하셔야 합니다.");
        }

        @Test
        @DisplayName("존재하지 않는 게시글로 채팅방 생성 시 예외")
        void createChatRoom_PostNotFound_ThrowsException() {
            // Given
            String userEmail = "test@test.com";
            Long postId = 999L;

            given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.createChatRoom(postId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("존재하지 않는 게시글입니다.");
        }
    }

    @Nested
    @DisplayName("채팅방 메시지 조회 테스트")
    class GetChatRoomMessagesTest {

        @Test
        @DisplayName("채팅방 메시지 조회 성공")
        void getChatRoomMessages_Success() {
            // Given
            Long chatRoomId = 1L;
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(true);
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.getId()))
                    .willReturn(true);

            List<Message> messages = Arrays.asList(testMessage);
            given(messageRepository.findByChatRoomId(chatRoomId)).willReturn(messages);

            // When
            List<MessageDto> result = chatService.getChatRoomMessages(chatRoomId, principal);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("테스트 메시지");
            assertThat(result.get(0).getSenderName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지 않는 채팅방 조회 시 예외")
        void getChatRoomMessages_ChatRoomNotFound_ThrowsException() {
            // Given
            Long chatRoomId = 999L;
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> chatService.getChatRoomMessages(chatRoomId, principal))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("존재하지 않는 채팅방입니다.");
        }

        @Test
        @DisplayName("채팅방 참여자가 아닌 경우 예외")
        void getChatRoomMessages_NotParticipant_ThrowsException() {
            // Given
            Long chatRoomId = 1L;
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(chatRoomRepository.existsById(chatRoomId)).willReturn(true);
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.getId()))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> chatService.getChatRoomMessages(chatRoomId, principal))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("채팅방 참여자만 메시지를 조회할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회 테스트")
    class GetMyChatRoomsTest {

        @Test
        @DisplayName("내 채팅방 목록 조회 성공")
        void getMyChatRooms_Success() {
            // Given
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            List<RoomParticipant> participations = Arrays.asList(testParticipant);
            given(roomParticipantRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(testUser.getId()))
                    .willReturn(participations);
            given(messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(testChatRoom.getId()))
                    .willReturn(testMessage);

            // When
            List<ChatRoomDto> result = chatService.getMyChatRooms(principal);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo(testChatRoom.getRoomName());
            assertThat(result.get(0).lastContent()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 채팅방 목록 조회 시 예외")
        void getMyChatRooms_NotLoggedIn_ThrowsException() {
            // Given
            given(principal.getName()).willReturn("");

            // When & Then
            assertThatThrownBy(() -> chatService.getMyChatRooms(principal))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("로그인 하셔야 합니다.");
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 테스트")
    class LeaveChatRoomTest {

        @Test
        @DisplayName("채팅방 나가기 성공")
        void leaveChatRoom_Success() {
            // Given
            Long chatRoomId = 1L;
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(roomParticipantRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.getId()))
                    .willReturn(Optional.of(testParticipant));
            given(roomParticipantRepository.existsByChatRoomIdAndIsActiveTrue(chatRoomId)).willReturn(true);
            given(roomParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoomId)).willReturn(Arrays.asList());

            // When
            assertThatCode(() -> chatService.leaveChatRoom(chatRoomId, principal))
                    .doesNotThrowAnyException();

            // Then
            verify(roomParticipantRepository).save(testParticipant);
            verify(redisMessageService).publishMessage(any(MessageDto.class));
            assertThat(testParticipant.isActive()).isFalse();
            assertThat(testParticipant.getLeftAt()).isNotNull();
        }

        @Test
        @DisplayName("참여하지 않은 채팅방 나가기 시 예외")
        void leaveChatRoom_NotParticipant_ThrowsException() {
            // Given
            Long chatRoomId = 1L;
            given(principal.getName()).willReturn("test@test.com");
            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(roomParticipantRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, testUser.getId()))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatService.leaveChatRoom(chatRoomId, principal))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("채팅방 참여자가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("참여자 확인 테스트")
    class IsParticipantTest {

        @Test
        @DisplayName("참여자인 경우 true 반환")
        void isParticipant_True() {
            // Given
            Long chatRoomId = 1L;
            Long memberId = 1L;
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId))
                    .willReturn(true);

            // When
            boolean result = chatService.isParticipant(chatRoomId, memberId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("참여자가 아닌 경우 false 반환")
        void isParticipant_False() {
            // Given
            Long chatRoomId = 1L;
            Long memberId = 1L;
            given(roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId))
                    .willReturn(false);

            // When
            boolean result = chatService.isParticipant(chatRoomId, memberId);

            // Then
            assertThat(result).isFalse();
        }
    }
}
