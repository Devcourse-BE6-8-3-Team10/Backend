package com.back.domain.chat.redis.listener

import com.back.domain.chat.chat.dto.MessageDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class RedisMessageSubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) : MessageListener {

    companion object {
        private val log = LoggerFactory.getLogger(RedisMessageSubscriber::class.java)
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        runCatching {
            log.info("=== Redis에서 메시지 수신 ===")

            // Redis에서 받은 메시지를 MessageDto로 변환
            val messageBody = String(message.body)
            log.info("수신된 메시지: {}", messageBody)

            // JSON 파싱
            val chatMessage: MessageDto = when {
                messageBody.startsWith("\"") && messageBody.endsWith("\"") -> {
                    // 이스케이프된 JSON 문자열을 언이스케이프
                    val unescapedJson = objectMapper.readValue(messageBody, String::class.java)
                    objectMapper.readValue(unescapedJson, MessageDto::class.java)
                }
                else -> {
                    // 일반 JSON 객체로 파싱
                    objectMapper.readValue(messageBody, MessageDto::class.java)
                }
            }

            log.info(
                "변환된 메시지 - 채팅방: {}, 발신자: {}, 내용: {}",
                chatMessage.chatRoomId,
                chatMessage.senderName,
                chatMessage.content
            )

            // WebSocket을 통해 클라이언트들에게 전송
            val destination = "/topic/chat/${chatMessage.chatRoomId}"
            messagingTemplate.convertAndSend(destination, chatMessage)

            log.info("WebSocket으로 메시지 전송 완료: {}", destination)

        }.onFailure { exception ->
            log.error("Redis 메시지 처리 중 에러 발생: {}", exception.message, exception)
        }
    }
}
