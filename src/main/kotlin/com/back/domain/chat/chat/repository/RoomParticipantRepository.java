package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByChatRoomIdAndMemberIdAndIsActiveTrue(Long chatRoomId, Long memberId);
    List<RoomParticipant> findByChatRoomIdAndIsActiveTrue(Long chatRoomId);
    List<RoomParticipant> findByChatRoomPostIdAndMemberIdAndIsActiveTrue(Long postId, Long memberId);

    List<RoomParticipant> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long id);

    Optional<RoomParticipant> findByChatRoomIdAndMemberIdAndIsActiveTrue(Long chatRoomId, Long id);

    boolean existsByChatRoomIdAndIsActiveTrue(Long chatRoomId);
    
    // 활성/비활성 무관하게 채팅방의 모든 참여자 조회
    List<RoomParticipant> findByChatRoomId(Long chatRoomId);
}
