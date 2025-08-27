package com.back.domain.post.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.Post.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 찜 수 기준 인기 게시글 상위 N개 조회
    List<Post> findTop10ByOrderByFavoriteCntDesc();
    // 최신 등록일 기준 정렬
    List<Post> findAllByOrderByCreatedAtDesc();
    // 특정 회원이 작성한 게시글
    List<Post> findByMember(Member member);
    // 상태 필터링 (사용할지 말지 모름)
    List<Post> findByStatus(Status status);
    // 키워드 검색 (사용할지 말지 모름)
    @Query("SELECT p FROM Post p WHERE p.title LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%')")
    List<Post> searchByKeyword(String keyword);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.favoriteCnt = p.favoriteCnt + 1 WHERE p.id = :postId")
    void increaseFavoriteCnt(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.favoriteCnt = p.favoriteCnt - 1 WHERE p.id = :postId AND p.favoriteCnt > 0")
    void decreaseFavoriteCnt(@Param("postId") Long postId);

    @Query("SELECT p.favoriteCnt FROM Post p WHERE p.id = :postId")
    int getFavoriteCnt(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);

}
