package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.entity.Message;
import com.back.domain.chat.chat.entity.RoomParticipant;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.entity.Status;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Chat Repository 테스트")
class ChatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    private Member testUser;
    private Member postAuthor;
    private Post testPost;
    private ChatRoom testChatRoom;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성 및 저장
        testUser = Member.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트유저")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
        testUser = memberRepository.save(testUser);

        postAuthor = Member.builder()
                .email("author@test.com")
                .password("password")
                .name("게시글작성자")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
        postAuthor = memberRepository.save(postAuthor);

        // 테스트 게시글 생성 및 저장
        testPost = Post.builder()
                .member(postAuthor)
                .title("테스트 게시글")
                .description("테스트 설명")
                .category(Post.Category.PRODUCT)
                .price(100000)
                .status(Post.Status.SALE)
                .build();
        testPost = postRepository.save(testPost);

        // 테스트 채팅방 생성 및 저장
        testChatRoom = new ChatRoom(testPost, testUser);
        testChatRoom = chatRoomRepository.save(testChatRoom);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ChatRoomRepository 테스트")
    class ChatRoomRepositoryTest {

        @Test
        @DisplayName("방 이름으로 채팅방 조회")
        void findByRoomName() {
            // When
            ChatRoom foundRoom = chatRoomRepository.findByRoomName(testChatRoom.getRoomName());

            // Then
            assertThat(foundRoom).isNotNull();
            assertThat(foundRoom.getRoomName()).isEqualTo(testChatRoom.getRoomName());
            assertThat(foundRoom.getPost().getId()).isEqualTo(testPost.getId());
            assertThat(foundRoom.getMember().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("게시글 ID로 채팅방 목록 조회")
        void findByPostId() {
            // Given - 같은 게시글에 대한 다른 채팅방 생성
            ChatRoom anotherChatRoom = new ChatRoom(testPost, postAuthor);
            chatRoomRepository.save(anotherChatRoom);

            // When
            List<ChatRoom> chatRooms = chatRoomRepository.findByPostId(testPost.getId());

            // Then
            assertThat(chatRooms).hasSize(2);
            assertThat(chatRooms).extracting("post.id")
                    .containsOnly(testPost.getId());
        }

        @Test
        @DisplayName("게시글 ID와 사용자 ID로 채팅방 조회")
        void findByPostIdAndMemberId() {
            // When
            Optional<ChatRoom> foundRoom = chatRoomRepository.findByPostIdAndMemberId(
                    testPost.getId(), testUser.getId());

            // Then
            assertThat(foundRoom).isPresent();
            assertThat(foundRoom.get().getPost().getId()).isEqualTo(testPost.getId());
            assertThat(foundRoom.get().getMember().getId()).isEqualTo(testUser.getId());
        }
    }

    @Nested
    @DisplayName("MessageRepository 테스트")
    class MessageRepositoryTest {

        @Test
        @DisplayName("채팅방 ID로 메시지 목록 조회")
        void findByChatRoomId() {
            // Given
            Message message1 = new Message(testUser, "첫번째 메시지");
            message1.setChatRoom(testChatRoom);
            messageRepository.save(message1);

            Message message2 = new Message(postAuthor, "두번째 메시지");
            message2.setChatRoom(testChatRoom);
            messageRepository.save(message2);

            // When
            List<Message> messages = messageRepository.findByChatRoomId(testChatRoom.getId());

            // Then
            assertThat(messages).hasSize(2);
            assertThat(messages).extracting("chatRoom.id")
                    .containsOnly(testChatRoom.getId());
            assertThat(messages).extracting("content")
                    .containsExactlyInAnyOrder("첫번째 메시지", "두번째 메시지");
        }

        @Test
        @DisplayName("채팅방의 마지막 메시지 조회")
        void findFirstByChatRoomIdOrderByCreatedAtDesc() {
            // Given
            Message firstMessage = new Message(testUser, "첫번째 메시지");
            firstMessage.setChatRoom(testChatRoom);
            messageRepository.save(firstMessage);

            entityManager.flush();

            Message lastMessage = new Message(postAuthor, "마지막 메시지");
            lastMessage.setChatRoom(testChatRoom);
            messageRepository.save(lastMessage);

            // When
            Message foundMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(testChatRoom.getId());

            // Then
            assertThat(foundMessage).isNotNull();
            assertThat(foundMessage.getContent()).isEqualTo("마지막 메시지");
        }
    }

    @Nested
    @DisplayName("RoomParticipantRepository 테스트")
    class RoomParticipantRepositoryTest {

        private RoomParticipant testParticipant;
        private RoomParticipant authorParticipant;

        @BeforeEach
        void setUpParticipants() {
            testParticipant = new RoomParticipant(testChatRoom, testUser);
            roomParticipantRepository.save(testParticipant);

            authorParticipant = new RoomParticipant(testChatRoom, postAuthor);
            roomParticipantRepository.save(authorParticipant);
        }

        @Test
        @DisplayName("채팅방과 사용자로 활성 참여자 존재 확인")
        void existsByChatRoomIdAndMemberIdAndIsActiveTrue() {
            // When
            boolean exists = roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(
                    testChatRoom.getId(), testUser.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("채팅방의 활성 참여자 목록 조회")
        void findByChatRoomIdAndIsActiveTrue() {
            // When
            List<RoomParticipant> participants = roomParticipantRepository.findByChatRoomIdAndIsActiveTrue(
                    testChatRoom.getId());

            // Then
            assertThat(participants).hasSize(2);
            assertThat(participants).extracting("member.id")
                    .containsExactlyInAnyOrder(testUser.getId(), postAuthor.getId());
        }
    }
}
