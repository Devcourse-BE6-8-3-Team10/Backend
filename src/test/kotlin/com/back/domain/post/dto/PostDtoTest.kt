package com.back.domain.post.dto

import com.back.domain.files.files.entity.Files
import com.back.domain.member.entity.Member
import com.back.domain.post.entity.FavoritePost
import com.back.domain.post.entity.Post
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class PostAndFavoriteDtosTest {

    // -----------------------------
    // 1) PostRequestDTO Bean Validation
    // -----------------------------
    @Test
    @DisplayName("PostRequestDTO: 유효 입력은 제약조건을 모두 통과한다")
    fun postRequestDTO_valid() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val dto = PostRequestDTO(
            title = "제목",
            description = "내용",
            category = "PRODUCT",
            price = 10_000
        )
        val violations = validator.validate(dto)
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("PostRequestDTO: 제목/내용/카테고리 공백 + 가격 0 이하면 제약조건 위반 발생")
    fun postRequestDTO_invalid() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val dto = PostRequestDTO(
            title = "  ",
            description = "",
            category = " ",
            price = 0
        )
        val violations = validator.validate(dto)
        val fields = violations.map { it.propertyPath.toString() }.toSet()
        assertThat(fields).contains("title", "description", "category", "price")
    }

    // -----------------------------
    // 2) PostListDTO.from 매핑 검증
    // -----------------------------
    @Test
    @DisplayName("PostListDTO.from: Post → 목록 DTO로 올바르게 매핑된다")
    fun postListDTO_from() {
        val (post, _, _) = makePostWithMemberAndFile()

        val dto = PostListDTO.from(post)

        assertThat(dto.id).isEqualTo(1L)
        assertThat(dto.title).isEqualTo("제목")
        assertThat(dto.price).isEqualTo(10_000)
        assertThat(dto.category).isEqualTo(Post.Category.PRODUCT.name)
        assertThat(dto.favoriteCnt).isEqualTo(3)
        assertThat(dto.createdAt).isEqualTo(FAKE_TIME)
        assertThat(dto.imageUrl).isEqualTo("https://cdn.example.com/file1.png")
    }

    // -----------------------------
    // 3) PostDetailDTO.of 매핑 검증
    // -----------------------------
    @Test
    @DisplayName("PostDetailDTO.of: Post → 상세 DTO로 올바르게 매핑된다")
    fun postDetailDTO_of() {
        val (post, _, _) = makePostWithMemberAndFile()

        val dto = PostDetailDTO.of(post, isLiked = true)

        assertThat(dto.id).isEqualTo(1L)
        assertThat(dto.writerName).isEqualTo("홍길동")
        assertThat(dto.title).isEqualTo("제목")
        assertThat(dto.description).isEqualTo("내용")
        assertThat(dto.category).isEqualTo(Post.Category.PRODUCT.label)
        assertThat(dto.price).isEqualTo(10_000)
        assertThat(dto.status).isEqualTo(Post.Status.SALE.label)
        assertThat(dto.favoriteCnt).isEqualTo(3)
        assertThat(dto.isLiked).isTrue()
        assertThat(dto.createdAt).isEqualTo(FAKE_TIME)
        assertThat(dto.modifiedAt).isEqualTo(FAKE_TIME)
    }

    // -----------------------------
    // 4) FavoritePostDTO.from 매핑 검증
    // -----------------------------
    @Test
    @DisplayName("FavoritePostDTO.from: FavoritePost → DTO로 올바르게 매핑된다")
    fun favoritePostDTO_from() {
        val (favoritePost, post) = makeFavoritePost()

        val dto = FavoritePostDTO.from(favoritePost, isLiked = true)

        assertThat(dto.postId).isEqualTo(1L)
        assertThat(dto.title).isEqualTo("제목")
        assertThat(dto.price).isEqualTo(10_000)
        assertThat(dto.favoriteCnt).isEqualTo(7)
        assertThat(dto.status).isEqualTo(Post.Status.SALE.label)
        assertThat(dto.isLiked).isTrue()
        assertThat(dto.createdAt).isEqualTo(FAKE_TIME)
    }

    // -----------------------------
    // 5) FavoriteResponseDTO 생성 검증
    // -----------------------------
    @Test
    @DisplayName("FavoriteResponseDTO: 생성자 값이 그대로 보존된다")
    fun favoriteResponseDTO_basic() {
        val dto = FavoriteResponseDTO(
            postId = 42L,
            isLiked = false,
            favoriteCnt = 3,
            message = "테스트 메시지"
        )

        assertThat(dto.postId).isEqualTo(42L)
        assertThat(dto.isLiked).isFalse()
        assertThat(dto.favoriteCnt).isEqualTo(3)
        assertThat(dto.message).isEqualTo("테스트 메시지")
    }

    // =====================================================================
    // 헬퍼들: 리플렉션으로 엔티티를 안전하게 구성 (스프링 컨텍스트 불필요)
    // =====================================================================

    private fun makePostWithMemberAndFile(): Triple<Post, Member, Files> {
        val member = newInstance(Member::class.java).apply {
            ReflectionTestUtils.setField(this, "id", 100L)
            ReflectionTestUtils.setField(this, "name", "홍길동")
            ReflectionTestUtils.setField(this, "email", "user@test.com")
            ReflectionTestUtils.setField(this, "password", "encoded")
        }

        val post = newInstance(Post::class.java).apply {
            ReflectionTestUtils.setField(this, "id", 1L)
            ReflectionTestUtils.setField(this, "member", member)
            ReflectionTestUtils.setField(this, "title", "제목")
            ReflectionTestUtils.setField(this, "description", "내용")
            ReflectionTestUtils.setField(this, "category", Post.Category.PRODUCT)
            ReflectionTestUtils.setField(this, "price", 10_000)
            ReflectionTestUtils.setField(this, "status", Post.Status.SALE)
            ReflectionTestUtils.setField(this, "favoriteCnt", 3)
            ReflectionTestUtils.setField(this, "createdAt", FAKE_TIME)
            ReflectionTestUtils.setField(this, "modifiedAt", FAKE_TIME)
        }

        val file = newInstance(Files::class.java).apply {
            ReflectionTestUtils.setField(this, "post", post)
            ReflectionTestUtils.setField(this, "fileName", "file1.png")
            ReflectionTestUtils.setField(this, "fileType", "image/png")
            ReflectionTestUtils.setField(this, "fileSize", 12345L)
            ReflectionTestUtils.setField(this, "fileUrl", "https://cdn.example.com/file1.png")
            ReflectionTestUtils.setField(this, "sortOrder", 1)
        }

        // Java List를 안전하게 캐스팅해서 추가
        @Suppress("UNCHECKED_CAST")
        val filesList = post.postFiles as MutableList<Files>
        filesList.add(file)

        return Triple(post, member, file)
    }

    private fun makeFavoritePost(): Pair<FavoritePost, Post> {
        val (post, member, _) = makePostWithMemberAndFile().let { Triple(it.first, it.second, it.third) }
        // favoriteCnt만 테스트 케이스에 맞춰 덮어쓰기
        ReflectionTestUtils.setField(post, "favoriteCnt", 7)

        val favoritePost = newInstance(FavoritePost::class.java).apply {
            ReflectionTestUtils.setField(this, "member", member)
            ReflectionTestUtils.setField(this, "post", post)
        }
        return favoritePost to post
    }

    private fun <T> newInstance(clazz: Class<T>): T {
        val ctor = clazz.getDeclaredConstructor()
        ctor.isAccessible = true
        return ctor.newInstance()
    }

    companion object {
        private val FAKE_TIME: LocalDateTime =
            LocalDateTime.of(2025, 1, 1, 12, 0, 0)
    }
}
