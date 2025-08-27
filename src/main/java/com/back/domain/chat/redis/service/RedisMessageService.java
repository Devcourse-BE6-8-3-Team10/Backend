package com.back.domain.chat.redis.service;

import com.back.domain.chat.chat.dto.MessageDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;
    private final ObjectMapper objectMapper;

    /*
     * Redis pub/sub을 통해 메시지 발행
     * @param message 발행할 메시지
     */
    public void publishMessage(MessageDto message) {
        try {
            log.info("=== Redis로 메시지 발행 ===");
            log.info("채팅방: {}, 발신자: {}, 내용: {}",
                    message.getChatRoomId(),
                    message.getSenderName(),
                    message.getContent());

            // MessageDto를 JSON 문자열로 변환
            String messageJson = objectMapper.writeValueAsString(message);

            // Redis 채널에 메시지 발행
            redisTemplate.convertAndSend(chatTopic.getTopic(), messageJson);

            log.info("Redis 메시지 발행 완료: 토픽={}, 메시지={}", chatTopic.getTopic(), messageJson);

        } catch (Exception e) {
            log.error("Redis 메시지 발행 중 에러 발생: {}", e.getMessage(), e);
            throw new ServiceException("400-1","메시지 발행 실패");
        }
    }

    /*
     * 특정 채팅방에 메시지 발행
     * @param chatRoomId 채팅방 ID
     * @param message 발행할 메시지
     */
    public void publishMessageToRoom(Long chatRoomId, MessageDto message) {
        try {
            log.info("=== 특정 채팅방으로 메시지 발행 ===");
            log.info("대상 채팅방: {}", chatRoomId);

            // 채팅방별 토픽으로 메시지 발행
            String roomTopic = "chat-room-" + chatRoomId;
            String messageJson = objectMapper.writeValueAsString(message);

            redisTemplate.convertAndSend(roomTopic, messageJson);

            log.info("채팅방별 메시지 발행 완료: 토픽={}", roomTopic);

        } catch (Exception e) {
            log.error("채팅방별 메시지 발행 중 에러 발생: {}", e.getMessage(), e);
            throw new ServiceException("400-1","메시지 발행 실패");
        }
    }
}
