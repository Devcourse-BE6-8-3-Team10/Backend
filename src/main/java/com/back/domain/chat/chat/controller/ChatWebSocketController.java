package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.MessageDto;
import com.back.domain.chat.chat.entity.Message;
import com.back.domain.chat.chat.service.ChatService;
import com.back.domain.chat.redis.service.RedisMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMessageService redisMessageService; // Redis 서비스 추가

    @MessageMapping("/sendMessage")
    public void sendMessage(MessageDto chatMessage) {
        log.info("=== WebSocket 메시지 수신 ===");
        log.info("sender: {}", chatMessage.getSenderName());
        log.info("senderEmail: {}", chatMessage.getSenderEmail());
        log.info("content: {}", chatMessage.getContent());
        log.info("senderId: {}", chatMessage.getSenderId());
        log.info("chatRoomId: {}", chatMessage.getChatRoomId());
        log.info("=================");

        try {
            // 1. 메시지 저장 (기존과 동일)
            Message savedMessage = chatService.saveMessage(chatMessage);
            log.info("메시지 저장 완료: {}", savedMessage.getId());

            // 2. 권한 체크: 발신자가 해당 채팅방 참여자인지 확인
            boolean isParticipant = chatService.isParticipant(chatMessage.getChatRoomId(), chatMessage.getSenderId());
            if (!isParticipant) {
                log.warn("권한 없음: 사용자 {}는 채팅방 {}의 참여자가 아닙니다",
                        chatMessage.getSenderId(), chatMessage.getChatRoomId());

                // 에러 메시지 전송
                sendErrorMessage(chatMessage.getSenderEmail(), "채팅방 참여자만 메시지를 보낼 수 있습니다.");
                return;
            }

            // 3. Redis pub/sub을 통해 메시지 발행 (새로운 방식!)
            log.info("=== Redis pub/sub으로 메시지 발행 시작 ===");
            redisMessageService.publishMessage(chatMessage);
            log.info("✅ Redis 메시지 발행 완료! 모든 서버 인스턴스에 전달됨");

        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 에러 발생: {}", e.getMessage(), e);

            // 에러 메시지는 발신자에게만 전송
            sendErrorMessage(chatMessage.getSenderEmail(), "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 에러 메시지를 특정 사용자에게 전송
     */
    private void sendErrorMessage(String userEmail, String errorMessage) {
        try {
            MessageDto errorMsg = new MessageDto();
            errorMsg.setSender("System");
            errorMsg.setContent(errorMessage);

            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/error",
                    errorMsg
            );
            log.info("에러 메시지 전송 완료: {} -> {}", userEmail, errorMessage);

        } catch (Exception errorSendFail) {
            log.error("에러 메시지 전송도 실패: {}", errorSendFail.getMessage(), errorSendFail);
        }
    }
}
