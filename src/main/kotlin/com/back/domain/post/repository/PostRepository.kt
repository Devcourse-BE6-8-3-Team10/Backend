package com.back.domain.post.repository

import com.back.domain.member.entity.Member
import com.back.domain.post.entity.Post
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface PostRepository : JpaRepository<Post, Long> {

    // 찜 수 기준 인기 게시글 상위 N개 조회
    fun findTop10ByOrderByFavoriteCntDesc(): List<Post>

    // 최신 등록일 기준 정렬
    fun findAllByOrderByCreatedAtDesc(): List<Post>

    // 특정 회원이 작성한 게시글
    fun findByMember(member: Member): List<Post>

    // 상태 필터링 (사용할지 말지 모름)
    fun findByStatus(status: Post.Status): List<Post>

    // 키워드 검색 (사용할지 말지 모름)
    @Query("SELECT p FROM Post p WHERE p.title LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%')")
    fun searchByKeyword(@Param("keyword") keyword: String): List<Post>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.favoriteCnt = p.favoriteCnt + 1 WHERE p.id = :postId")
    fun increaseFavoriteCnt(@Param("postId") postId: Long)

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.favoriteCnt = p.favoriteCnt - 1 WHERE p.id = :postId AND p.favoriteCnt > 0")
    fun decreaseFavoriteCnt(@Param("postId") postId: Long)

    @Query("SELECT p.favoriteCnt FROM Post p WHERE p.id = :postId")
    fun getFavoriteCnt(@Param("postId") postId: Long): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :postId")
    fun findByIdForUpdate(@Param("postId") postId: Long): Post?
}
