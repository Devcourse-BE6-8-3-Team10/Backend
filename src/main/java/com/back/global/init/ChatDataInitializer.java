package com.back.global.init;

import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.entity.Message;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.chat.chat.repository.MessageRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Profile({"dev", "prod"})
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // DevDataInitializer 이후에 실행
public class ChatDataInitializer implements ApplicationRunner {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 채팅 데이터 초기화 시작 =====");
        initChatRooms();
        initChatMessage();
        log.info("===== 채팅 데이터 초기화 완료 =====");
    }

    @Transactional
    private void initChatMessage() {
        // 먼저 실제 Member들의 ID를 확인
        log.info("=== 현재 존재하는 Member들 확인 ===");
        var allMembers = memberRepository.findAll();
        for (var member : allMembers) {
            log.info("Member ID: {}, Email: {}, Name: {}", member.getId(), member.getEmail(), member.getName());
        }

        // 실제 존재하는 첫 번째 Member의 ID 사용
        if (!allMembers.isEmpty()) {
            Long firstMemberId = allMembers.get(0).getId();
            createChatMessageIfNotExists("유저1", "안녕하세요! 첫 번째 메시지입니다.", firstMemberId, 1L);
        } else {
            log.warn("Member가 존재하지 않아 메시지를 생성할 수 없습니다.");
        }
    }

    private void createChatMessageIfNotExists(String senderName, String content, Long senderId, Long chatRoomId) {
        try {
            // 디버깅 로그 추가
            log.info("메시지 생성 시도: senderId={}, chatRoomId={}", senderId, chatRoomId);

            // Member와 ChatRoom을 Repository에서 조회
            Optional<Member> member = memberRepository.findById(senderId);
            Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);

            log.info("조회 결과: member.isPresent()={}, chatRoom.isPresent()={}",
                    member.isPresent(), chatRoom.isPresent());

            if (member.isPresent() && chatRoom.isPresent()) {
                Message message = new Message();
                message.setSender(member.get());
                message.setChatRoom(chatRoom.get());
                message.setContent(content);

                messageRepository.save(message);
                log.info("채팅 메시지 '{}' 이 생성되었습니다. (발신자: {})", content, senderName);
            } else {
                log.warn("메시지 생성 실패: Member(ID:{}) 또는 ChatRoom(ID:{})을 찾을 수 없습니다.", senderId, chatRoomId);
            }
        } catch (Exception e) {
            log.error("메시지 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    @Transactional
    public void initChatRooms() {
        // 기본 채팅방들 생성
        createChatRoomIfNotExists("일반 채팅방");
        createChatRoomIfNotExists("자유 대화방");
        createChatRoomIfNotExists("질문&답변방");
    }

    private void createChatRoomIfNotExists(String roomName) {
        // 같은 이름의 채팅방이 없으면 생성
        if (chatRoomRepository.findByRoomName(roomName) == null) {
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setRoomName(roomName);
            // post는 일단 null로 설정 (나중에 게시글과 연결)
            chatRoom.setPost(null);

            chatRoomRepository.save(chatRoom);
            log.info("채팅방 '{}' 이 생성되었습니다.", roomName);
        }
    }
}
