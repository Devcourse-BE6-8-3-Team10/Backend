package com.back.domain.files.files.entity

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("Files Entity 테스트")
class FilesTest {

    private lateinit var mockMember: Member
    private lateinit var mockPost: Post

    @BeforeEach
    fun setUp() {
        // Member 객체 생성
        mockMember = Member(
            email = "test@test.com",
            password = "password123",
            name = "testUser",
            profileUrl = null,
            role = Role.USER,
            status = Status.ACTIVE
        )

        // Post 객체 생성
        mockPost = Post.builder()
            .member(mockMember)
            .title("테스트 게시글")
            .description("테스트 내용")
            .category(Post.Category.PRODUCT)
            .price(50000)
            .status(Post.Status.SALE)
            .build()

        // BaseEntity 필드 설정
        setBaseEntityFields(mockMember, 1L, LocalDateTime.now())
        setBaseEntityFields(mockPost, 1L, LocalDateTime.now())
    }

    @Test
    @DisplayName("Files 엔티티 정상 생성 테스트")
    fun testFilesCreation() {
        // given
        val fileName = "test-document.pdf"
        val fileType = "application/pdf"
        val fileSize = 2048L
        val fileUrl = "https://example.com/test-document.pdf"
        val sortOrder = 1

        // when
        val files = Files(
            post = mockPost,
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSize,
            fileUrl = fileUrl,
            sortOrder = sortOrder
        )

        // then
        assertAll(
            "Files 엔티티 필드 검증",
            { assertEquals(mockPost, files.post, "Post 연관관계가 올바르게 설정되어야 함") },
            { assertEquals(fileName, files.fileName, "파일명이 올바르게 설정되어야 함") },
            { assertEquals(fileType, files.fileType, "파일 타입이 올바르게 설정되어야 함") },
            { assertEquals(fileSize, files.fileSize, "파일 크기가 올바르게 설정되어야 함") },
            { assertEquals(fileUrl, files.fileUrl, "파일 URL이 올바르게 설정되어야 함") },
            { assertEquals(sortOrder, files.sortOrder, "정렬 순서가 올바르게 설정되어야 함") }
        )
    }

    @Test
    @DisplayName("Files 엔티티 Post 연관관계 테스트")
    fun testPostRelationship() {
        // given
        val files = Files(
            post = mockPost,
            fileName = "relation-test.jpg",
            fileType = "image/jpeg",
            fileSize = 1024L,
            fileUrl = "https://example.com/relation-test.jpg",
            sortOrder = 1
        )

        // when & then
        assertNotNull(files.post, "Post 연관관계가 null이 아니어야 함")
        assertEquals(mockPost.id, files.post.id, "Post ID가 일치해야 함")
        assertEquals(mockPost.title, files.post.title, "Post 제목이 일치해야 함")
        assertEquals(mockPost.member.email, files.post.member.email, "Post의 Member 정보에 접근 가능해야 함")
    }

    @Test
    @DisplayName("Files 엔티티 필드 유효성 테스트")
    fun testFieldValidation() {
        // given - 다양한 파일 타입 테스트
        val testCases = listOf(
            Triple("document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 5120L),
            Triple("image.png", "image/png", 2048L),
            Triple("video.mp4", "video/mp4", 104857600L), // 100MB
            Triple("archive.zip", "application/zip", 1048576L) // 1MB
        )

        testCases.forEachIndexed { index, (fileName, fileType, fileSize) ->
            // when
            val files = Files(
                post = mockPost,
                fileName = fileName,
                fileType = fileType,
                fileSize = fileSize,
                fileUrl = "https://example.com/$fileName",
                sortOrder = index + 1
            )

            // then
            assertAll(
                "파일 $fileName 검증",
                { assertTrue(files.fileName.isNotEmpty(), "파일명이 비어있지 않아야 함") },
                { assertTrue(files.fileType.isNotEmpty(), "파일 타입이 비어있지 않아야 함") },
                { assertTrue(files.fileSize > 0, "파일 크기가 0보다 커야 함") },
                { assertTrue(files.fileUrl.startsWith("https://"), "파일 URL이 올바른 형식이어야 함") },
                { assertTrue(files.sortOrder > 0, "정렬 순서가 0보다 커야 함") }
            )
        }
    }

    @Test
    @DisplayName("Files 엔티티 최소값 생성 테스트")
    fun testMinimalFilesCreation() {
        // given
        val minimalFiles = Files(
            post = mockPost,
            fileName = "minimal.txt",
            fileType = "text/plain",
            fileSize = 1L,
            fileUrl = "https://example.com/minimal.txt",
            sortOrder = 1
        )

        // when & then
        assertNotNull(minimalFiles, "최소 필수값으로 객체가 생성되어야 함")
        assertEquals("minimal.txt", minimalFiles.fileName, "최소값 파일명이 올바르게 설정되어야 함")
        assertEquals("text/plain", minimalFiles.fileType, "최소값 파일 타입이 올바르게 설정되어야 함")
        assertEquals(1L, minimalFiles.fileSize, "최소값 파일 크기가 올바르게 설정되어야 함")
        assertEquals("https://example.com/minimal.txt", minimalFiles.fileUrl, "최소값 파일 URL이 올바르게 설정되어야 함")
        assertEquals(1, minimalFiles.sortOrder, "최소값 정렬 순서가 올바르게 설정되어야 함")
    }

    @Test
    @DisplayName("Files 엔티티 BaseEntity 상속 테스트")
    fun testBaseEntityInheritance() {
        // given
        val files = Files(
            post = mockPost,
            fileName = "inheritance-test.txt",
            fileType = "text/plain",
            fileSize = 512L,
            fileUrl = "https://example.com/inheritance-test.txt",
            sortOrder = 1
        )

        val testId = 99L
        val testCreatedAt = LocalDateTime.now()

        // when
        setBaseEntityFields(files, testId, testCreatedAt)

        // then
        assertEquals(testId, files.id, "BaseEntity의 id 필드가 올바르게 설정되어야 함")
        assertEquals(testCreatedAt, files.createdAt, "BaseEntity의 createdAt 필드가 올바르게 설정되어야 함")
    }

    @Test
    @DisplayName("Files 엔티티 정렬 순서 테스트")
    fun testSortOrderFunctionality() {
        // given
        val filesList = mutableListOf<Files>()
        
        repeat(5) { index ->
            val files = Files(
                post = mockPost,
                fileName = "file-${index + 1}.txt",
                fileType = "text/plain",
                fileSize = (index + 1) * 100L,
                fileUrl = "https://example.com/file-${index + 1}.txt",
                sortOrder = index + 1
            )
            filesList.add(files)
        }

        // when
        val sortedFiles = filesList.sortedBy { it.sortOrder }

        // then
        assertEquals(5, sortedFiles.size, "정렬된 파일 목록의 크기가 5여야 함")
        
        sortedFiles.forEachIndexed { index, files ->
            assertEquals(index + 1, files.sortOrder, "정렬 순서가 올바르게 정렬되어야 함")
            assertEquals("file-${index + 1}.txt", files.fileName, "파일명이 순서대로 정렬되어야 함")
        }
    }

    @Test
    @DisplayName("Files 엔티티 큰 파일 크기 처리 테스트")
    fun testLargeFileSizeHandling() {
        // given
        val largeFileSize = 5368709120L // 5GB

        // when
        val files = Files(
            post = mockPost,
            fileName = "large-file.zip",
            fileType = "application/zip",
            fileSize = largeFileSize,
            fileUrl = "https://example.com/large-file.zip",
            sortOrder = 1
        )

        // then
        assertEquals(largeFileSize, files.fileSize, "큰 파일 크기도 올바르게 처리되어야 함")
        assertTrue(files.fileSize > Integer.MAX_VALUE, "Long 타입으로 큰 파일 크기를 처리할 수 있어야 함")
    }

    @Test
    @DisplayName("Files 엔티티 특수 문자 파일명 처리 테스트")
    fun testSpecialCharacterFileName() {
        // given
        val specialFileNames = listOf(
            "한글파일명.pdf",
            "file with spaces.docx",
            "file-with-dashes_and_underscores.txt",
            "파일명(괄호포함).jpg",
            "file.name.with.dots.png"
        )

        specialFileNames.forEach { fileName ->
            // when
            val files = Files(
                post = mockPost,
                fileName = fileName,
                fileType = "application/octet-stream",
                fileSize = 1024L,
                fileUrl = "https://example.com/${fileName.hashCode()}",
                sortOrder = 1
            )

            // then
            assertEquals(fileName, files.fileName, "특수 문자가 포함된 파일명이 올바르게 저장되어야 함")
            assertNotNull(files.fileUrl, "파일 URL이 null이 아니어야 함")
        }
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
