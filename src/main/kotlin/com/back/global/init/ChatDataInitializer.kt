package com.back.global.init

import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.Message
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.repository.MessageRepository
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("dev", "prod")
@Component
@Order(2)
class ChatDataInitializer(
    private val chatRoomRepository: ChatRoomRepository,
    private val messageRepository: MessageRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository
) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(ChatDataInitializer::class.java)
    }

    override fun run(args: ApplicationArguments) {
        log.info("===== 채팅 데이터 초기화 시작 =====")
        initChatRooms()
        initChatMessage()
        log.info("===== 채팅 데이터 초기화 완료 =====")
    }

    @Transactional
    private fun initChatMessage() {
        log.info("=== 현재 존재하는 Member들 확인 ===")
        val allMembers = memberRepository.findAll()

        allMembers.forEach { member ->
            log.info("Member ID: {}, Email: {}, Name: {}",
                member.getId(), member.email, member.name
            )
        }

        if (allMembers.isNotEmpty()) {
            val firstMemberId = allMembers.first().getId()
            createChatMessageIfNotExists("유저1", "안녕하세요! 첫 번째 메시지입니다.", firstMemberId, 1L)
        } else {
            log.warn("Member가 존재하지 않아 메시지를 생성할 수 없습니다.")
        }
    }

    private fun createChatMessageIfNotExists(senderName: String, content: String, senderId: Long, chatRoomId: Long) {
        try {
            log.info("메시지 생성 시도: senderId={}, chatRoomId={}", senderId, chatRoomId)

            val member = memberRepository.findById(senderId)
            val chatRoom = chatRoomRepository.findById(chatRoomId)

            log.info("조회 결과: member.isPresent={}, chatRoom.isPresent={}",
                member.isPresent, chatRoom.isPresent)

            if (member.isPresent && chatRoom.isPresent) {
                val message = Message().apply {
                    updateMember(member.get())
                    updateChatRoom(chatRoom.get())
                    updateContent(content)
                }

                messageRepository.save(message)
                log.info("채팅 메시지 '{}' 이 생성되었습니다. (발신자: {})", content, senderName)
            } else {
                log.warn("메시지 생성 실패: Member(ID:{}) 또는 ChatRoom(ID:{})을 찾을 수 없습니다.",
                    senderId, chatRoomId)
            }
        } catch (e: Exception) {
            log.error("메시지 생성 중 오류 발생: {}", e.message)
        }
    }

    @Transactional
    private fun initChatRooms() {
        createChatRoomIfNotExists("일반 채팅방")
        createChatRoomIfNotExists("자유 대화방")
        createChatRoomIfNotExists("질문&답변방")
    }

    private fun createChatRoomIfNotExists(roomName: String) {
        postRepository.findById(1L).ifPresent { post ->
            if (chatRoomRepository.findByRoomName(roomName) == null) {
                val chatRoom = ChatRoom().apply {
                    updateRoomName(roomName)
                    updatePost(post)
                }

                chatRoomRepository.save(chatRoom)
                log.info("채팅방 '{}' 이 생성되었습니다.", roomName)
            }
        }
    }
}
