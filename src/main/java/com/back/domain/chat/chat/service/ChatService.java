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
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PostRepository postRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RedisMessageService redisMessageService; // Redis 서비스 추가

    @Transactional
    public Message saveMessage(MessageDto chatMessage) {
        Member sender = memberRepository.findById(chatMessage.getSenderId())
                .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 사용자입니다."));

        ChatRoom chatRoom = chatRoomRepository.findById(chatMessage.getChatRoomId())
                .orElseThrow(() -> new ServiceException("404-4", "존재하지 않는 채팅방입니다."));

        Message message = new Message(chatMessage , sender);
        message.setChatRoom(chatRoom);

        return messageRepository.save(message);
    }
    @Transactional
    public boolean isParticipant(Long chatRoomId, Long memberId) {
        return roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId);
    }
    @Transactional
    public List<MessageDto> getChatRoomMessages(Long chatRoomId, Principal principal) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 사용자입니다."));
        Long requesterId = member.getId();

        // 채팅방 존재 확인
        if( !chatRoomRepository.existsById(chatRoomId)) {
            throw new ServiceException("404-4", "존재하지 않는 채팅방입니다.");
        }

        // 권한 체크 추가
        if(!isParticipant(chatRoomId, requesterId)) {
            throw new ServiceException("403-1", "채팅방 참여자만 메시지를 조회할 수 있습니다.");
        }


        // 메시지 조회 (시간순 정렬)
        List<Message> messages = messageRepository.findByChatRoomId(chatRoomId);

        messages.sort(Comparator.comparing(Message::getCreatedAt));
        // Entity -> DTO 변환
        return messages.stream()
                .map(message -> {
                    return new MessageDto(message.getSender().getName(),
                            message.getContent(),
                            message.getSender().getId(),
                            message.getChatRoom().getId());
                })
                .toList();
    }
    @Transactional
    public Long createChatRoom(Long postId, String userEmail) {
        if(userEmail == null || userEmail.isEmpty()) {
            throw new ServiceException("400-1", "로그인 하셔야 합니다.");
        }

        // 이메일로 Member 엔티티 조회 (JWT의 principal.getName()은 이메일을 반환)
        Member requester = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 사용자입니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 게시글입니다."));

        Member postAuthor = post.getMember();

        log.debug("=== 채팅방 생성 시작 ===");
        log.debug("요청자: " + requester.getEmail() + " (ID: " + requester.getId() + ")");
        log.debug("게시글 작성자: " + postAuthor.getEmail() + " (ID: " + postAuthor.getId() + ")");
        log.debug("게시글 ID: " + postId);


//        log.debug("=== 전체 멤버 확인 ===");
//        List<Member> allMembers = memberRepository.findAll();
//        for (Member m : allMembers) {
//            log.debug("멤버: " + m.getEmail() + " (ID: " + m.getId() + ")");
//        }


        Long existingChatRoomId = findExistingChatRoom(postId, requester.getId(), postAuthor.getId());

        if (existingChatRoomId != null) {
            log.debug("기존 채팅방 발견: " + existingChatRoomId);
            return existingChatRoomId;
        }

        log.debug("새 채팅방 생성 시작");

        // 기존 1대1 채팅방이 없다면 새로 생성
        ChatRoom chatRoom = new ChatRoom(post, requester);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 정확히 2명만 참여자로 추가
        roomParticipantRepository.save(new RoomParticipant(savedChatRoom, requester));
        roomParticipantRepository.save(new RoomParticipant(savedChatRoom, postAuthor));

        log.debug("새 채팅방 생성 완료: " + savedChatRoom.getId());
        return savedChatRoom.getId();
    }

    @Transactional
    public Long findExistingChatRoom(Long postId, Long requesterId, Long postAuthorId) {
        // 해당 게시글에 대한 요청자가 만든 채팅방이 있는지 확인 (활성/비활성 무관)
        List<ChatRoom> allPostChatRooms = chatRoomRepository.findByPostId(postId);
        
        for (ChatRoom chatRoom : allPostChatRooms) {
            // 이 채팅방의 모든 참여자 확인 (활성/비활성 무관)
            List<RoomParticipant> allParticipants = roomParticipantRepository
                .findByChatRoomId(chatRoom.getId());
                
            log.debug("채팅방 " + chatRoom.getId() + " 전체 참여자 수: " + allParticipants.size());
                
            // 참여자가 정확히 2명이고, 요청자와 postAuthor가 모두 포함되어 있는지 확인
            if (allParticipants.size() == 2) {
                boolean hasRequester = allParticipants.stream()
                    .anyMatch(p -> p.getMember().getId().equals(requesterId));
                boolean hasPostAuthor = allParticipants.stream()
                    .anyMatch(p -> p.getMember().getId().equals(postAuthorId));
                    
                if (hasRequester && hasPostAuthor) {
                    // 기존 채팅방 발견 - 두 참여자 모두 다시 활성화
                    for (RoomParticipant participant : allParticipants) {
                        participant.setActive(true);
                        participant.setLeftAt(null); // 나간 시간 초기화
                    }
                    roomParticipantRepository.saveAll(allParticipants);
                    
                    log.debug("기존 채팅방 재활용: " + chatRoom.getId());
                    return chatRoom.getId();
                }
            }
        }
        return null; // 기존 채팅방 없음
    }

    @Transactional
    public List<ChatRoomDto> getMyChatRooms(Principal principal) {
        if(principal == null || principal.getName() == null || principal.getName().isEmpty()) {
            throw new ServiceException("400-1", "로그인 하셔야 합니다.");
        }

        // 이메일로 Member 엔티티 조회 (JWT의 principal.getName()은 이메일을 반환)
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 사용자입니다."));


        // 개선
        List<RoomParticipant> participations = roomParticipantRepository
                .findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(member.getId());


        // RoomParticipant에서 ChatRoom 추출 및 DTO 변환
        return participations.stream()
                .map(participation -> {
                    ChatRoom chatRoom = participation.getChatRoom();

                    // 마지막 메시지 조회
                    Message lastMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId());
                    String lastContent = (lastMessage != null) ? lastMessage.getContent() : "대화를 시작해보세요.";

                    return new ChatRoomDto(
                            chatRoom.getId(),
                            chatRoom.getRoomName(),
                            chatRoom.getPost().getId(),
                            lastContent
                    );
                })
                .toList();
    }

    @Transactional
    public void leaveChatRoom(Long chatRoomId, Principal principal) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ServiceException("404-3", "존재하지 않는 사용자입니다."));

        RoomParticipant participant = roomParticipantRepository
                .findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, member.getId())
                .orElseThrow(() -> new ServiceException("404-5", "채팅방 참여자가 아닙니다."));

        // 나가기 전에 다른 참여자들에게 알림 메시지 전송
        sendLeaveNotificationToOtherParticipants(chatRoomId, member);

        participant.setActive(false);
        participant.setLeftAt(LocalDateTime.now());
        roomParticipantRepository.save(participant);

        boolean hasActiveParticipants = roomParticipantRepository.existsByChatRoomIdAndIsActiveTrue(chatRoomId);

        int activeCount = roomParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoomId).size();

        if(!hasActiveParticipants) {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new ServiceException("404-4", "존재하지 않는 채팅방입니다."));
            chatRoomRepository.delete(chatRoom);
        }
    }

    /**
     * 채팅방을 나가는 사용자 외의 다른 참여자들에게 나가기 알림 전송
     */
    private void sendLeaveNotificationToOtherParticipants(Long chatRoomId, Member leavingMember) {
        try {
            log.info("=== 채팅방 나가기 알림 전송 시작 ===");
            log.info("나가는 사용자: {} (ID: {})", leavingMember.getName(), leavingMember.getId());
            log.info("채팅방 ID: {}", chatRoomId);

            // 나가기 알림 메시지 생성
            MessageDto leaveNotification = new MessageDto();
            leaveNotification.setSender("System");
            leaveNotification.setSenderName("시스템");
            leaveNotification.setContent(leavingMember.getName() + "님이 채팅방을 나갔습니다.");
            leaveNotification.setSenderId(-1L); // 시스템 메시지 구분용
            leaveNotification.setChatRoomId(chatRoomId);
            leaveNotification.setMessageType("LEAVE_NOTIFICATION"); // 메시지 타입 추가

            // Redis를 통해 알림 메시지 발송
            redisMessageService.publishMessage(leaveNotification);

            log.info("✅ 채팅방 나가기 알림 전송 완료");

        } catch (Exception e) {
            log.error("❌ 채팅방 나가기 알림 전송 실패: {}", e.getMessage(), e);
            // 알림 전송 실패해도 나가기 로직은 계속 진행
        }
    }
}
