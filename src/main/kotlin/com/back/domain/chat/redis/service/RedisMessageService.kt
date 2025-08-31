package com.back.domain.chat.redis.service

import com.back.domain.chat.chat.dto.MessageDto
import com.back.global.exception.ServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Service

@Service
class RedisMessageService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val chatTopic: ChannelTopic,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val log = LoggerFactory.getLogger(RedisMessageService::class.java)
    }

    /**
     * Redis pub/sub을 통해 메시지 발행
     * @param message 발행할 메시지
     */
    fun publishMessage(message: MessageDto) {
        runCatching {
            log.info("=== Redis로 메시지 발행 ===")
            log.info(
                "채팅방: {}, 발신자: {}, 내용: {}",
                message.chatRoomId,
                message.senderName,
                message.content
            )

            // MessageDto를 JSON 문자열로 변환
            val messageJson = objectMapper.writeValueAsString(message)

            // Redis 채널에 메시지 발행
            redisTemplate.convertAndSend(chatTopic.topic, messageJson)

            log.info("Redis 메시지 발행 완료: 토픽={}, 메시지={}", chatTopic.topic, messageJson)

        }.onFailure { exception ->
            log.error("Redis 메시지 발행 중 에러 발생: {}", exception.message, exception)
            throw ServiceException("400-1", "메시지 발행 실패")
        }
    }

    /**
     * 특정 채팅방에 메시지 발행
     * @param chatRoomId 채팅방 ID
     * @param message 발행할 메시지
     */
    fun publishMessageToRoom(chatRoomId: Long, message: MessageDto) {
        runCatching {
            log.info("=== 특정 채팅방으로 메시지 발행 ===")
            log.info("대상 채팅방: {}", chatRoomId)

            // 채팅방별 토픽으로 메시지 발행
            val roomTopic = "chat-room-$chatRoomId"
            val messageJson = objectMapper.writeValueAsString(message)

            redisTemplate.convertAndSend(roomTopic, messageJson)

            log.info("채팅방별 메시지 발행 완료: 토픽={}", roomTopic)

        }.onFailure { exception ->
            log.error("채팅방별 메시지 발행 중 에러 발생: {}", exception.message, exception)
            throw ServiceException("400-1", "메시지 발행 실패")
        }
    }
}
