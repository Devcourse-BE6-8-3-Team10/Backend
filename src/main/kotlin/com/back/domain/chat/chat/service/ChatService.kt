package com.back.domain.chat.chat.service

import com.back.domain.chat.chat.dto.ChatRoomDto
import com.back.domain.chat.chat.dto.MessageDto
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.Message
import com.back.domain.chat.chat.entity.RoomParticipant
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.repository.MessageRepository
import com.back.domain.chat.chat.repository.RoomParticipantRepository
import com.back.domain.chat.redis.service.RedisMessageService
import com.back.domain.member.entity.Member
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.repository.PostRepository
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal
import java.time.LocalDateTime

@Service
class ChatService(
    private val messageRepository: MessageRepository,
    private val memberRepository: MemberRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val postRepository: PostRepository,
    private val roomParticipantRepository: RoomParticipantRepository,
    private val redisMessageService: RedisMessageService
) {
    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)
    }

    @Transactional
    fun saveMessage(chatMessage: MessageDto): Message {
        val sender = memberRepository.findById(chatMessage.senderId)
            .orElseThrow { ServiceException("404-3", "존재하지 않는 사용자입니다.") }

        val chatRoom = chatRoomRepository.findById(chatMessage.chatRoomId)
            .orElseThrow { ServiceException("404-4", "존재하지 않는 채팅방입니다.") }

        val message = Message(chatMessage, sender).apply {
            setChatRoom(chatRoom)
        }

        return messageRepository.save(message)
    }

    @Transactional
    fun isParticipant(chatRoomId: Long, memberId: Long): Boolean =
        roomParticipantRepository.existsByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId)

    @Transactional
    fun getChatRoomMessages(chatRoomId: Long, principal: Principal): List<MessageDto> {
        val member = memberRepository.findByEmail(principal.getName())
            .orElseThrow { ServiceException("404-3", "존재하지 않는 사용자입니다.") }
        val requesterId = member.getId()

        // 채팅방 존재 확인
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw ServiceException("404-4", "존재하지 않는 채팅방입니다.")
        }

        // 권한 체크 추가
        if (!isParticipant(chatRoomId, requesterId)) {
            throw ServiceException("403-1", "채팅방 참여자만 메시지를 조회할 수 있습니다.")
        }

        // 메시지 조회 (시간순 정렬)
        return messageRepository.findByChatRoomId(chatRoomId)
            .sortedBy { it.getCreatedAt() }
            .map { message ->
                MessageDto(
                    message.getSender().getName(),
                    message.getContent(),
                    message.getSender().getId(),
                    message.getChatRoom().getId()
                )
            }
    }

    @Transactional
    fun createChatRoom(postId: Long, userEmail: String): Long {
        if (userEmail.isBlank()) {
            throw ServiceException("400-1", "로그인 하셔야 합니다.")
        }

        // 이메일로 Member 엔티티 조회
        val requester = memberRepository.findByEmail(userEmail)
            .orElseThrow { ServiceException("404-3", "존재하지 않는 사용자입니다.") }

        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 게시글입니다.") }

        val postAuthor = post.getMember()

        log.debug("=== 채팅방 생성 시작 ===")
        log.debug("요청자: {} (ID: {})", requester.getEmail(), requester.getId())
        log.debug("게시글 작성자: {} (ID: {})", postAuthor.getEmail(), postAuthor.getId())
        log.debug("게시글 ID: {}", postId)

        findExistingChatRoom(postId, requester.getId(), postAuthor.getId())?.let { existingChatRoomId ->
            log.debug("기존 채팅방 발견: {}", existingChatRoomId)
            return existingChatRoomId
        }

        log.debug("새 채팅방 생성 시작")

        // 기존 1대1 채팅방이 없다면 새로 생성
        val chatRoom = ChatRoom(post, requester)
        val savedChatRoom = chatRoomRepository.save(chatRoom)

        // 정확히 2명만 참여자로 추가
        roomParticipantRepository.save(RoomParticipant(savedChatRoom, requester))
        roomParticipantRepository.save(RoomParticipant(savedChatRoom, postAuthor))

        log.debug("새 채팅방 생성 완료: {}", savedChatRoom.getId())
        return savedChatRoom.getId()
    }

    @Transactional
    fun findExistingChatRoom(postId: Long, requesterId: Long, postAuthorId: Long): Long? {
        // 해당 게시글에 대한 요청자가 만든 채팅방이 있는지 확인
        val allPostChatRooms = chatRoomRepository.findByPostId(postId)

        for (chatRoom in allPostChatRooms) {
            // 이 채팅방의 모든 참여자 확인
            val allParticipants = roomParticipantRepository.findByChatRoomId(chatRoom.id)

            log.debug("채팅방 {} 전체 참여자 수: {}", chatRoom.id, allParticipants.size)

            // 참여자가 정확히 2명이고, 요청자와 postAuthor가 모두 포함되어 있는지 확인
            if (allParticipants.size == 2) {
                val hasRequester = allParticipants.any { it.member.id == requesterId }
                val hasPostAuthor = allParticipants.any { it.member.id == postAuthorId }

                if (hasRequester && hasPostAuthor) {
                    // 기존 채팅방 발견 - 두 참여자 모두 다시 활성화
                    allParticipants.forEach { participant ->
                        participant.isActive = true
                        participant.leftAt = null // 나간 시간 초기화
                    }
                    roomParticipantRepository.saveAll(allParticipants)

                    log.debug("기존 채팅방 재활용: {}", chatRoom.id)
                    return chatRoom.id
                }
            }
        }
        return null // 기존 채팅방 없음
    }

    @Transactional
    fun getMyChatRooms(principal: Principal): List<ChatRoomDto> {
        if (principal.getName().isBlank()) {
            throw ServiceException("400-1", "로그인 하셔야 합니다.")
        }

        // 이메일로 Member 엔티티 조회
        val member = memberRepository.findByEmail(principal.getName())
            .orElseThrow { ServiceException("404-3", "존재하지 않는 사용자입니다.") }

        val participations = roomParticipantRepository
            .findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(member.getId())

        // RoomParticipant에서 ChatRoom 추출 및 DTO 변환
        return participations.map { participation ->
            val chatRoom = participation.getChatRoom()
            // 마지막 메시지 조회
            val lastMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId())
            val lastContent = lastMessage?.getContent() ?: "대화를 시작해보세요."

            ChatRoomDto(
                chatRoom.getId(),
                chatRoom.getRoomName(),
                chatRoom.getPost().getId(),
                lastContent
            )
        }
    }

    @Transactional
    fun leaveChatRoom(chatRoomId: Long, principal: Principal) {
        val member = memberRepository.findByEmail(principal.getName())
            .orElseThrow { ServiceException("404-3", "존재하지 않는 사용자입니다.") }

        val participant = roomParticipantRepository
            .findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, member.getId())
            .orElseThrow { ServiceException("404-5", "채팅방 참여자가 아닙니다.") }

        // 나가기 전에 다른 참여자들에게 알림 메시지 전송
        sendLeaveNotificationToOtherParticipants(chatRoomId, member)

        participant.apply {
            setActive(false)
            setLeftAt(LocalDateTime.now())
        }
        roomParticipantRepository.save(participant)

        val hasActiveParticipants = roomParticipantRepository.existsByChatRoomIdAndIsActiveTrue(chatRoomId)

        if (!hasActiveParticipants) {
            val chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow { ServiceException("404-4", "존재하지 않는 채팅방입니다.") }
            chatRoomRepository.delete(chatRoom)
        }
    }

    private fun sendLeaveNotificationToOtherParticipants(chatRoomId: Long, leavingMember: Member) {
        try {
            log.info("=== 채팅방 나가기 알림 전송 시작 ===")
            log.info("나가는 사용자: {} (ID: {})", leavingMember.getName(), leavingMember.getId())
            log.info("채팅방 ID: {}", chatRoomId)

            // 나가기 알림 메시지 생성
            val leaveNotification = MessageDto(
                senderId = -1L,
                chatRoomId = chatRoomId,
                senderName = "시스템",
                senderEmail = "system@devteam10.org",
                content = "${leavingMember.getName()}님이 채팅방을 나갔습니다.",
                messageType = "LEAVE_NOTIFICATION"
            )

            // Redis를 통해 알림 메시지 발송
            redisMessageService.publishMessage(leaveNotification)

            log.info("✅ 채팅방 나가기 알림 전송 완료")
        } catch (e: Exception) {
            log.error("❌ 채팅방 나가기 알림 전송 실패: {}", e.message, e)
            // 알림 전송 실패해도 나가기 로직은 계속 진행
        }
    }
}
