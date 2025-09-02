package com.back.domain.post.service

import com.back.domain.member.entity.Member
import com.back.domain.post.dto.FavoriteResponseDTO
import com.back.domain.post.dto.PostDetailDTO
import com.back.domain.post.dto.PostDetailDTO.Companion.of
import com.back.domain.post.dto.PostListDTO
import com.back.domain.post.dto.PostRequestDTO
import com.back.domain.post.entity.FavoritePost
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.FavoritePostRepository
import com.back.domain.post.repository.PostRepository
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val favoritePostRepository: FavoritePostRepository,
    private val rq: Rq
) {

    // 게시글 생성
    @Transactional
    fun createPost(dto: PostRequestDTO): PostDetailDTO {
        val member = currentMemberOrThrow

        // 카테고리 변환 예외 처리
        val category = Post.Category.from(dto.category)
            ?: throw ServiceException("400", "유효하지 않은 카테고리입니다.")

        val post = Post(
            member,
            dto.title,
            dto.description,
            category,
            dto.price,
            Post.Status.SALE
        )

        val saved = postRepository.save(post)
        return of(saved, false)
    }

    // 게시글 수정
    @Transactional
    fun updatePost(postId: Long, dto: PostRequestDTO): PostDetailDTO {
        val member = currentMemberOrThrow
        val post = getPostOrThrow(postId)

        // 본인 게시글인지 확인
        if (post.member.id != member.id) {
            throw ServiceException("403", "자신의 게시글만 수정할 수 있습니다.")
        }

        // 카테고리 예외처리
        val category = Post.Category.from(dto.category)
            ?: throw ServiceException("400", "유효하지 않은 카테고리입니다.")

        // 수정 값 적용
        post.updatePost(dto.title, dto.description, category, dto.price)
        return of(post, favoritePostRepository.existsByMemberAndPost(member, post))
    }

    // 게시글 삭제
    @Transactional
    fun deletePost(postId: Long): RsData<String> {
        val member = currentMemberOrThrow

        // 예외처리
        val post = postRepository.findById(postId).orElse(null)
            ?: throw ServiceException("404", "이미 삭제되었거나 존재하지 않는 게시글입니다.")

        // 본인 게시글인지 확인
        if (post.member.id != member.id) {
            throw ServiceException("403", "자신의 게시글만 삭제할 수 있습니다.")
        }

        postRepository.delete(post)
        return RsData(ResultCode.SUCCESS, "게시글 삭제 완료", null)
    }

    // 게시글 목록 조회
    @get:Transactional(readOnly = true)
    val postList: List<PostListDTO>
        get() = postRepository.findAllByOrderByCreatedAtDesc()
            .map { PostListDTO.from(it) }

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    fun getPostDetail(postId: Long): RsData<PostDetailDTO> {
        val member = currentMemberOrThrow
        val post = getPostOrThrow(postId)

        val isLiked = favoritePostRepository.existsByMemberAndPost(member, post)
        return RsData(ResultCode.SUCCESS, "게시글 조회 성공", of(post, isLiked))
    }

    // 인기 게시글 조회
    @get:Transactional(readOnly = true)
    val top10PopularPosts: List<PostListDTO>
        get() = postRepository.findTop10ByOrderByFavoriteCntDesc()
            .map { PostListDTO.from(it) }

    // 찜 등록 해제
    @Transactional
    fun toggleFavorite(postId: Long): FavoriteResponseDTO {
        val member = currentMemberOrThrow
        val post = getPostForUpdateOrThrow(postId)

        if (post.member == member) {
            return FavoriteResponseDTO(
                post.id,
                false,
                post.favoriteCnt,
                "자신의 게시글은 찜할 수 없습니다."
            )
        }

        val alreadyLiked = favoritePostRepository.existsByMemberAndPost(member, post)

        return if (alreadyLiked) {
            favoritePostRepository.deleteByMemberAndPost(member, post)
            postRepository.decreaseFavoriteCnt(postId)
            val newFavoriteCnt = postRepository.getFavoriteCnt(postId)

            FavoriteResponseDTO(
                post.id,
                false,
                newFavoriteCnt,
                "'${post.title}' 찜 해제 완료"
            )
        } else {
            favoritePostRepository.save(
                FavoritePost(
                    member = member,
                    post = post
                )
            )

            postRepository.increaseFavoriteCnt(postId)
            val newFavoriteCnt = postRepository.getFavoriteCnt(postId)

            FavoriteResponseDTO(
                post.id,
                true,
                newFavoriteCnt,
                "'${post.title}' 찜 등록 완료"
            )
        }
    }

    // 찜 목록 조회
    @get:Transactional(readOnly = true)
    val favoritePosts: List<PostListDTO>
        get() {
            val member = currentMemberOrThrow
            val favoritePosts = favoritePostRepository.findByMemberOrderByPostCreatedAtDesc(member)
            return favoritePosts
                .mapNotNull { it.post }
                .map { PostListDTO.from(it) }
        }

    // 내 게시글 목록 조회
    @get:Transactional(readOnly = true)
    val myPosts: List<PostListDTO>
        get() {
            val member = currentMemberOrThrow
            return postRepository.findByMember(member)
                .map { PostListDTO.from(it) }
        }

    // ------------------------------------------------------------------
    private val currentMemberOrThrow: Member
        get() = rq.member ?: throw ServiceException("401", "로그인이 필요합니다.")

    // 게시글 조회 에러
    private fun getPostOrThrow(postId: Long): Post {
        return postRepository.findById(postId).orElse(null)
            ?: throw ServiceException("404", "게시글이 존재하지 않습니다.")
    }

    // 찜 기능 시 동기화 문제 처리 락
    private fun getPostForUpdateOrThrow(postId: Long): Post {
        return postRepository.findByIdForUpdate(postId)
            ?: throw ServiceException("404", "게시글이 존재하지 않습니다.")
    }
}
