package com.back.domain.post.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.FavoritePost;
import com.back.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritePostRepository extends JpaRepository<FavoritePost, Long> {

    // 특정 회원이 찜한 게시글 목록
    List<FavoritePost> findByMember(Member member);
    // 찜 여부 확인 (중복 방지)
    boolean existsByMemberAndPost(Member member, Post post);
    // 게시글 삭제 시 찜 삭제
    void deleteAllByPost(Post post);
    // 찜 취소 기능
    void deleteByMemberAndPost(Member member, Post post);
    //찜 목록 조회
    List<FavoritePost> findByMemberOrderByPostCreatedAtDesc(Member member);
}
