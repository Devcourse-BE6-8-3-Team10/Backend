package com.back.domain.post.service;

import com.back.domain.member.entity.Member;
import com.back.domain.post.dto.FavoriteResponseDTO;
import com.back.domain.post.dto.PostDetailDTO;
import com.back.domain.post.dto.PostListDTO;
import com.back.domain.post.dto.PostRequestDTO;
import com.back.domain.post.entity.FavoritePost;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.FavoritePostRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.ResultCode;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FavoritePostRepository favoritePostRepository;
    private final Rq rq;

    //게시글 생성
    @Transactional
    public PostDetailDTO createPost(PostRequestDTO dto) {
        Member member = getCurrentMemberOrThrow();

        // 카테고리 변환 예외 처리
        Post.Category category = Post.Category.from(dto.category())
                .orElseThrow(() -> new ServiceException("400", "유효하지 않은 카테고리입니다."));

        Post post = Post.builder()
                .title(dto.title())
                .description(dto.description())
                .category(category)
                .price(dto.price())
                .member(member)
                .status(Post.Status.SALE)
                .build();

        Post saved = postRepository.save(post);
        return new PostDetailDTO(saved, false);
    }

    //게시글 수정
    @Transactional
    public PostDetailDTO updatePost(Long postId, PostRequestDTO dto) {
        Member member = getCurrentMemberOrThrow();
        Post post = getPostOrThrow(postId);

        // 본인 게시글인지 확인
        if (!post.getMember().getId().equals(member.getId())) {
            throw new ServiceException("403", "자신의 게시글만 수정할 수 있습니다.");
        }

        // 카테고리 예외처리
        Post.Category category = Post.Category.from(dto.category())
                .orElseThrow(() -> new ServiceException("400", "유효하지 않은 카테고리입니다."));

        // 수정 값 적용
        post.updatePost(dto.title(), dto.description(), category, dto.price());
        return new PostDetailDTO(post, favoritePostRepository.existsByMemberAndPost(member, post));
    }

    // 게시글 삭제
    @Transactional
    public RsData<String> deletePost(Long postId) {
        Member member = getCurrentMemberOrThrow();

        //예외처리
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("404", "이미 삭제되었거나 존재하지 않는 게시글입니다."));

        // 본인 게시글인지 확인
        if (!post.getMember().getId().equals(member.getId())) {
            throw new ServiceException("403", "자신의 게시글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
        return new RsData<>(ResultCode.SUCCESS, "게시글 삭제 완료", null);
    }


    //게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostListDTO> getPostList() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostListDTO::new)
                .toList();
    }

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    public RsData<PostDetailDTO> getPostDetail(Long postId) {
        Member member = getCurrentMemberOrThrow();
        Post post = getPostOrThrow(postId);

        boolean isLiked = favoritePostRepository.existsByMemberAndPost(member, post);
        return new RsData<>(ResultCode.SUCCESS, "게시글 조회 성공", new PostDetailDTO(post, isLiked));
    }

    //인기 게시글 조회
    @Transactional(readOnly = true)
    public List<PostListDTO> getTop10PopularPosts() {
        return postRepository.findTop10ByOrderByFavoriteCntDesc()
                .stream()
                .map(PostListDTO::new)
                .toList();
    }

    //찜 등록 해제
    @Transactional
    public FavoriteResponseDTO toggleFavorite(Long postId) {
        Member member = getCurrentMemberOrThrow();
        Post post = getPostForUpdateOrThrow(postId);

        if (post.getMember().equals(member)) {
            return new FavoriteResponseDTO(post.getId(), false, post.getFavoriteCnt(), "자신의 게시글은 찜할 수 없습니다.");
        }

        boolean alreadyLiked = favoritePostRepository.existsByMemberAndPost(member, post);

        if (alreadyLiked) {
            favoritePostRepository.deleteByMemberAndPost(member, post);
            postRepository.decreaseFavoriteCnt(postId);
            int newFavoriteCnt = postRepository.getFavoriteCnt(postId);

            return new FavoriteResponseDTO(
                    post.getId(), false, newFavoriteCnt,
                    String.format("'%s' 찜 해제 완료", post.getTitle())
            );
        } else {
            favoritePostRepository.save(FavoritePost.builder()
                    .member(member)
                    .post(post)
                    .build());

            postRepository.increaseFavoriteCnt(postId);
            int newFavoriteCnt = postRepository.getFavoriteCnt(postId);

            return new FavoriteResponseDTO(
                    post.getId(), true, newFavoriteCnt,
                    String.format("'%s' 찜 등록 완료", post.getTitle())
            );
        }
    }

    //찜 목록 조회
    @Transactional(readOnly = true)
    public List<PostListDTO> getFavoritePosts() {
        Member member = getCurrentMemberOrThrow();
        List<FavoritePost> favoritePosts = favoritePostRepository.findByMemberOrderByPostCreatedAtDesc(member);
        return favoritePosts.stream()
                .map(FavoritePost::getPost)
                .map(PostListDTO::new)
                .toList();
    }

    // 내 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostListDTO> getMyPosts() {
        Member member = getCurrentMemberOrThrow();
        return postRepository.findByMember(member)
                .stream()
                .map(PostListDTO::new)
                .toList();
    }

    //------------------------------------------------------------------

    //현재 로그인 유저 확인
    private Member getCurrentMemberOrThrow() {
        Member member = rq.getMember();
        if (member == null) {
            throw new ServiceException("401", "로그인이 필요합니다.");
        }
        return member;
    }

    // 게시글 조회 에러
    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("404", "게시글이 존재하지 않습니다."));
    }

    // 찜 기능 시 동기화 문제 처리 락
    private Post getPostForUpdateOrThrow(Long postId) {
        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ServiceException("404", "게시글이 존재하지 않습니다."));
    }

}