package com.back.domain.files.files.dto

import com.back.domain.files.files.entity.Files
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("FileUploadResponseDto 테스트")
class FileUploadResponseDtoTest {

    private lateinit var mockPost: Post
    private lateinit var mockMember: Member
    private lateinit var testFiles: Files

    @BeforeEach
    fun setUp() {
        // Member 객체 생성 (Post 생성에 필요)
        mockMember = Member(
            email = "test@test.com",
            password = "password123",
            name = "testUser",
            profileUrl = null,
            role = Role.USER,
            status = Status.ACTIVE
        )

        // Post 객체 생성 (Files 생성에 필요)
        mockPost = Post.builder()
            .member(mockMember)
            .title("테스트 게시글")
            .description("테스트 내용")
            .category(Post.Category.PRODUCT)
            .price(50000)
            .status(Post.Status.SALE)
            .build()

        // Files 객체 생성
        testFiles = Files(
            post = mockPost,
            fileName = "test-file.jpg",
            fileType = "image/jpeg",
            fileSize = 1024L,
            fileUrl = "https://example.com/test-file.jpg",
            sortOrder = 1
        )

        // BaseEntity의 필드들을 수동으로 설정 (실제로는 JPA가 자동 설정)
        // reflection을 사용하여 private 필드에 값 설정
        setBaseEntityFields(testFiles, 1L, LocalDateTime.now())
        setBaseEntityFields(mockPost, 1L, LocalDateTime.now())
    }

    @Test
    @DisplayName("DTO 직접 생성 테스트")
    fun testDirectCreation() {
        // given
        val id = 1L
        val postId = 1L
        val fileName = "test.jpg"
        val fileType = "image/jpeg"
        val fileSize = 2048L
        val fileUrl = "https://example.com/test.jpg"
        val sortOrder = 1
        val createdAt = LocalDateTime.now()

        // when
        val dto = FileUploadResponseDto(
            id = id,
            postId = postId,
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSize,
            fileUrl = fileUrl,
            sortOrder = sortOrder,
            createdAt = createdAt
        )

        // then
        assertEquals(id, dto.id)
        assertEquals(postId, dto.postId)
        assertEquals(fileName, dto.fileName)
        assertEquals(fileType, dto.fileType)
        assertEquals(fileSize, dto.fileSize)
        assertEquals(fileUrl, dto.fileUrl)
        assertEquals(sortOrder, dto.sortOrder)
        assertEquals(createdAt, dto.createdAt)
    }

    @Test
    @DisplayName("Files Entity에서 DTO 변환 테스트")
    fun testFromEntity() {
        // when
        val dto = FileUploadResponseDto.from(testFiles)

        // then
        assertEquals(testFiles.id, dto.id)
        assertEquals(testFiles.post.id, dto.postId)
        assertEquals(testFiles.fileName, dto.fileName)
        assertEquals(testFiles.fileType, dto.fileType)
        assertEquals(testFiles.fileSize, dto.fileSize)
        assertEquals(testFiles.fileUrl, dto.fileUrl)
        assertEquals(testFiles.sortOrder, dto.sortOrder)
        assertEquals(testFiles.createdAt, dto.createdAt)
    }

    @Test
    @DisplayName("from 메서드가 올바른 값들을 매핑하는지 테스트")
    fun testFromMethodMapping() {
        // given
        val expectedFileName = "sample.pdf"
        val expectedFileType = "application/pdf"
        val expectedFileSize = 5120L
        val expectedSortOrder = 3

        val files = Files(
            post = mockPost,
            fileName = expectedFileName,
            fileType = expectedFileType,
            fileSize = expectedFileSize,
            fileUrl = "https://example.com/sample.pdf",
            sortOrder = expectedSortOrder
        )
        setBaseEntityFields(files, 2L, LocalDateTime.now())

        // when
        val dto = FileUploadResponseDto.from(files)

        // then
        assertAll(
            "DTO 필드 검증",
            { assertEquals(2L, dto.id, "ID가 올바르게 매핑되어야 함") },
            { assertEquals(mockPost.id, dto.postId, "Post ID가 올바르게 매핑되어야 함") },
            { assertEquals(expectedFileName, dto.fileName, "파일명이 올바르게 매핑되어야 함") },
            { assertEquals(expectedFileType, dto.fileType, "파일 타입이 올바르게 매핑되어야 함") },
            { assertEquals(expectedFileSize, dto.fileSize, "파일 크기가 올바르게 매핑되어야 함") },
            { assertEquals(expectedSortOrder, dto.sortOrder, "정렬 순서가 올바르게 매핑되어야 함") },
            { assertNotNull(dto.createdAt, "생성일이 null이 아니어야 함") }
        )
    }

    @Test
    @DisplayName("데이터 클래스 특성 테스트")
    fun testDataClassProperties() {
        // given
        val dto1 = FileUploadResponseDto(
            id = 1L,
            postId = 1L,
            fileName = "test.jpg",
            fileType = "image/jpeg",
            fileSize = 1024L,
            fileUrl = "https://example.com/test.jpg",
            sortOrder = 1,
            createdAt = LocalDateTime.now()
        )

        val dto2 = FileUploadResponseDto(
            id = 1L,
            postId = 1L,
            fileName = "test.jpg",
            fileType = "image/jpeg",
            fileSize = 1024L,
            fileUrl = "https://example.com/test.jpg",
            sortOrder = 1,
            createdAt = dto1.createdAt
        )

        // when & then
        assertEquals(dto1, dto2, "같은 값을 가진 DTO는 equals()에서 true를 반환해야 함")
        assertEquals(dto1.hashCode(), dto2.hashCode(), "같은 값을 가진 DTO는 같은 hashCode를 가져야 함")
        assertNotNull(dto1.toString(), "toString()이 null을 반환하지 않아야 함")
        assertTrue(dto1.toString().contains("FileUploadResponseDto"), "toString()에 클래스명이 포함되어야 함")
    }

    /**
     * BaseEntity의 private 필드들을 reflection을 사용해서 설정하는 헬퍼 메서드
     */
    private fun setBaseEntityFields(entity: Any, id: Long, createdAt: LocalDateTime) {
        try {
            val clazz = entity.javaClass.superclass // BaseEntity 클래스
            
            // id 필드 설정
            val idField = clazz.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(entity, id)
            
            // createdAt 필드 설정
            val createdAtField = clazz.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.set(entity, createdAt)
            
        } catch (e: Exception) {
            throw RuntimeException("BaseEntity 필드 설정 실패", e)
        }
    }
}
