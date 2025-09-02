package com.back.domain.post.repository

import com.back.domain.member.entity.Member
import com.back.domain.post.entity.FavoritePost
import com.back.domain.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface FavoritePostRepository : JpaRepository<FavoritePost, Long> {

    // 특정 회원이 찜한 게시글 목록
    fun findByMember(member: Member): List<FavoritePost>

    // 찜 여부 확인 (중복 방지)
    fun existsByMemberAndPost(member: Member, post: Post): Boolean

    // 게시글 삭제 시 찜 삭제
    fun deleteAllByPost(post: Post)

    // 찜 취소 기능
    fun deleteByMemberAndPost(member: Member, post: Post)

    // 찜 목록 조회
    fun findByMemberOrderByPostCreatedAtDesc(member: Member): List<FavoritePost>
}
