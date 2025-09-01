package com.back.domain.post.dto

import com.back.domain.files.files.entity.Files
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class PostDtosTest {

    // -----------------------------
    // PostRequestDTO Bean Validation
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
    // PostListDTO.from 매핑 검증
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
    // PostDetailDTO.of 매핑 검증
    // -----------------------------
    @Test
    @DisplayName("PostDetailDTO.of: Post → 상세 DTO로 올바르게 매핑된다")
    fun postDetailDTO_of() {
        val (post, member, _) = makePostWithMemberAndFile()

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
    // 테스트용 헬퍼
    // -----------------------------
    private fun makePostWithMemberAndFile(): Triple<Post, Member, Files> {
        // 1) Member는 빌더로 생성
        val member = Member(
            "test@example.com",
            "encoded",
            "홍길동",
            null,
            Role.USER,
            Status.ACTIVE
        ).also {
            ReflectionTestUtils.setField(it, "id", 100L)
        }

        // 2) Post 생성
        val post = Post(
            member,
            "제목",
            "내용",
            Post.Category.PRODUCT,
            10_000,
            Post.Status.SALE,
            0,
            mutableListOf(),
            null,
            mutableListOf(),
            mutableListOf()
        )

        ReflectionTestUtils.setField(post, "id", 1L)
        ReflectionTestUtils.setField(post, "favoriteCnt", 3)
        ReflectionTestUtils.setField(post, "createdAt", FAKE_TIME)
        ReflectionTestUtils.setField(post, "modifiedAt", FAKE_TIME)

        // 테스트에서 직접 생성자 사용
        val file = Files(
            post = post,
            fileName = "file1.png",
            fileType = "image/png",
            fileSize = 12345L,
            fileUrl = "https://cdn.example.com/file1.png",
            sortOrder = 1
        )
        post.postFiles.add(file)
        return Triple(post, member, file)
    }


    companion object {
        private val FAKE_TIME: LocalDateTime =
            LocalDateTime.of(2025, 1, 1, 12, 0, 0)
    }
}
