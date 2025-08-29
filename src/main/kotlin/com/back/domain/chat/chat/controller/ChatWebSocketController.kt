package com.back.domain.chat.chat.controller

import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.chat.chat.service.ChatService
import com.back.domain.chat.redis.service.RedisMessageService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisMessageService: RedisMessageService
) {
    companion object {
        private val log = LoggerFactory.getLogger(ChatWebSocketController::class.java)
    }

    @MessageMapping("/sendMessage")
    fun sendMessage(chatMessage: MessageDto) {
        log.info("=== WebSocket 메시지 수신 ===")
        log.info("sender: {}", chatMessage.senderName)
        log.info("senderEmail: {}", chatMessage.senderEmail)
        log.info("content: {}", chatMessage.content)
        log.info("senderId: {}", chatMessage.senderId)
        log.info("chatRoomId: {}", chatMessage.chatRoomId)
        log.info("=================")

        try {
            // 1. 메시지 저장 (기존과 동일)
            val savedMessage = chatService.saveMessage(chatMessage)
            log.info("메시지 저장 완료: {}", savedMessage.getId())

            // 2. 권한 체크: 발신자가 해당 채팅방 참여자인지 확인
            val isParticipant = chatService.isParticipant(chatMessage.chatRoomId, chatMessage.senderId)
            if (!isParticipant) {
                log.warn(
                    "권한 없음: 사용자 {}는 채팅방 {}의 참여자가 아닙니다",
                    chatMessage.senderId, chatMessage.chatRoomId
                )

                // 에러 메시지 전송
                sendErrorMessage(chatMessage.senderEmail, "채팅방 참여자만 메시지를 보낼 수 있습니다.")
                return
            }

            // 3. Redis pub/sub을 통해 메시지 발행 (새로운 방식!)
            log.info("=== Redis pub/sub으로 메시지 발행 시작 ===")
            redisMessageService.publishMessage(chatMessage)
            log.info("✅ Redis 메시지 발행 완료! 모든 서버 인스턴스에 전달됨")
        } catch (e: Exception) {
            log.error("❌ 메시지 처리 중 에러 발생: {}", e.message, e)

            // 에러 메시지는 발신자에게만 전송
            sendErrorMessage(chatMessage.senderEmail, "메시지 전송에 실패했습니다: ${e.message}")
        }
    }

    private fun sendErrorMessage(userEmail: String, errorMessage: String) {
        try {
            val errorMsg = MessageDto("System", errorMessage)

            messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/error",
                errorMsg
            )
            log.info("에러 메시지 전송 완료: {} -> {}", userEmail, errorMessage)
        } catch (errorSendFail: Exception) {
            log.error("에러 메시지 전송도 실패: {}", errorSendFail.message, errorSendFail)
        }
    }
}
