package com.back.domain.files.files.service

import com.back.domain.files.files.entity.Files
import com.back.domain.files.files.repository.FilesRepository
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.back.util.MockitoKotlinUtils.any
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AsyncFileService 테스트")
internal class AsyncFileServiceTest {

    @Mock
    private lateinit var filesRepository: FilesRepository

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @Mock
    private lateinit var postRepository: PostRepository

    @InjectMocks
    private lateinit var asyncFileService: AsyncFileService

    private lateinit var testMember: Member
    private lateinit var testPost: Post
    private lateinit var testFile: Files

    @BeforeEach
    fun setUp() {
        // 테스트 멤버 생성
        testMember = Member(
            email = "test@test.com",
            password = "password",
            name = "테스트사용자",
            profileUrl = null,
            role = Role.USER,
            status = Status.ACTIVE
        )
        ReflectionTestUtils.setField(testMember, "id", 1L)

        // 테스트 게시글 생성
        testPost = Post (
            testMember,
            "테스트 게시글",
            "테스트 설명",
            Post.Category.PRODUCT,
            100000,
            Post.Status.SALE
        )
        ReflectionTestUtils.setField(testPost, "id", 1L)

        // 테스트 파일 생성
        testFile = Files(
            testPost,
            "test.jpg",
            "image/jpeg",
            1024L,
            "/uploads/test.jpg",
            1
        )
        ReflectionTestUtils.setField(testFile, "id", 1L)
    }

    @Nested
    @DisplayName("파일 업로드 비동기 처리 테스트")
    internal inner class UploadFilesAsyncTest {

        @Test
        @DisplayName("정상적인 파일 업로드 처리")
        fun t1() {
            // Given
            val postId = 1L
            val fileData = AsyncFileService.FileData(
                "test.jpg",
                "image/jpeg",
                "test content".toByteArray()
            )
            val fileDataList = listOf(fileData)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(filesRepository.findLastByPostIdWithLock(eq(postId), any<PageRequest>()))
                .willReturn(emptyList())
            given(fileStorageService.storeFile(
                any<ByteArray>(),
                anyString(),
                anyString(),
                anyString()
            )).willReturn("/uploads/test.jpg")
            given(filesRepository.save(any<Files>())).willReturn(testFile)

            // When
            asyncFileService.uploadFilesAsync(postId, fileDataList)

            // Then
            verify(postRepository).findById(postId)
            verify(filesRepository).findLastByPostIdWithLock(eq(postId), any<PageRequest>())
            verify(fileStorageService).storeFile(any<ByteArray>(), anyString(), anyString(), anyString())
            verify(filesRepository).save(any<Files>())
        }

        @Test
        @DisplayName("빈 파일 리스트로 업로드 처리 시 조기 종료")
        fun t2() {
            // Given
            val postId = 1L
            val emptyFileDataList = emptyList<AsyncFileService.FileData>()

            // When
            asyncFileService.uploadFilesAsync(postId, emptyFileDataList)

            // Then
            verify(postRepository, never()).findById(anyLong())
            verify(filesRepository, never()).findLastByPostIdWithLock(anyLong(), any<PageRequest>())
            verify(fileStorageService, never()).storeFile(any<ByteArray>(), anyString(), anyString(), anyString())
            verify(filesRepository, never()).save(any<Files>())
        }

        @Test
        @DisplayName("존재하지 않는 게시글로 파일 업로드 시 예외")
        fun t3() {
            // Given
            val postId = 999L
            val fileData = AsyncFileService.FileData(
                "test.jpg",
                "image/jpeg",
                "test content".toByteArray()
            )
            val fileDataList = listOf(fileData)

            given(postRepository.findById(postId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { 
                asyncFileService.uploadFilesAsync(postId, fileDataList) 
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("비동기 처리 중 게시글을 찾을 수 없습니다: $postId")
        }

        @Test
        @DisplayName("기존 파일이 있는 경우 sortOrder 증가 처리")
        fun t4() {
            // Given
            val postId = 1L
            val fileData = AsyncFileService.FileData(
                "test2.jpg",
                "image/jpeg",
                "test content 2".toByteArray()
            )
            val fileDataList = listOf(fileData)

            val existingFile = Files(
                testPost,
                "existing.jpg",
                "image/jpeg",
                512L,
                "/uploads/existing.jpg",
                3
            )

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(filesRepository.findLastByPostIdWithLock(eq(postId), any<PageRequest>()))
                .willReturn(listOf(existingFile))
            given(fileStorageService.storeFile(any<ByteArray>(), anyString(), anyString(), anyString()))
                .willReturn("/uploads/test2.jpg")
            given(filesRepository.save(any<Files>())).willReturn(testFile)

            // When
            asyncFileService.uploadFilesAsync(postId, fileDataList)

            // Then
            verify(filesRepository).save(any<Files>())
        }

        @Test
        @DisplayName("파일 저장 실패 시 해당 파일 건너뛰기")
        fun t5() {
            // Given
            val postId = 1L
            val fileData1 = AsyncFileService.FileData(
                "fail.jpg",
                "image/jpeg",
                "fail content".toByteArray()
            )
            val fileData2 = AsyncFileService.FileData(
                "success.jpg",
                "image/jpeg",
                "success content".toByteArray()
            )
            val fileDataList = listOf(fileData1, fileData2)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(filesRepository.findLastByPostIdWithLock(eq(postId), any<PageRequest>()))
                .willReturn(emptyList())

            // storeFile 호출에 대한 순차적 응답 설정
            given(fileStorageService.storeFile(any<ByteArray>(), anyString(), anyString(), anyString()))
                .willThrow(RuntimeException("파일 저장 실패"))
                .willReturn("/uploads/success.jpg")

            given(filesRepository.save(any<Files>())).willReturn(testFile)

            // When
            asyncFileService.uploadFilesAsync(postId, fileDataList)

            // Then
            verify(filesRepository, times(1)).save(any<Files>())
        }

        @Test
        @DisplayName("여러 파일 정상 업로드 처리")
        fun t6() {
            // Given
            val postId = 1L
            val fileData1 = AsyncFileService.FileData(
                "file1.jpg",
                "image/jpeg",
                "content1".toByteArray()
            )
            val fileData2 = AsyncFileService.FileData(
                "file2.png",
                "image/png",
                "content2".toByteArray()
            )
            val fileDataList = listOf(fileData1, fileData2)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(filesRepository.findLastByPostIdWithLock(eq(postId), any<PageRequest>()))
                .willReturn(emptyList())
            given(fileStorageService.storeFile(any<ByteArray>(), anyString(), anyString(), anyString()))
                .willReturn("/uploads/file1.jpg")
                .willReturn("/uploads/file2.png")
            given(filesRepository.save(any<Files>())).willReturn(testFile)

            // When
            asyncFileService.uploadFilesAsync(postId, fileDataList)

            // Then
            verify(fileStorageService, times(2)).storeFile(any<ByteArray>(), anyString(), anyString(), anyString())
            verify(filesRepository, times(2)).save(any<Files>())
        }
    }

    @Nested
    @DisplayName("FileData 클래스 테스트")
    internal inner class FileDataTest {

        @Test
        @DisplayName("FileData equals 메서드 테스트")
        fun t7() {
            // Given
            val content = "test content".toByteArray()
            val fileData1 = AsyncFileService.FileData("test.jpg", "image/jpeg", content)
            val fileData2 = AsyncFileService.FileData("test.jpg", "image/jpeg", content)
            val fileData3 = AsyncFileService.FileData("test2.jpg", "image/jpeg", content)

            // When & Then
            assertThat(fileData1).isEqualTo(fileData2)
            assertThat(fileData1).isNotEqualTo(fileData3)
        }

        @Test
        @DisplayName("FileData hashCode 메서드 테스트")
        fun t8() {
            // Given
            val content = "test content".toByteArray()
            val fileData1 = AsyncFileService.FileData("test.jpg", "image/jpeg", content)
            val fileData2 = AsyncFileService.FileData("test.jpg", "image/jpeg", content)

            // When & Then
            assertThat(fileData1.hashCode()).isEqualTo(fileData2.hashCode())
        }

        @Test
        @DisplayName("FileData 다른 내용으로 생성 시 equals false")
        fun t9() {
            // Given
            val content1 = "test content 1".toByteArray()
            val content2 = "test content 2".toByteArray()
            val fileData1 = AsyncFileService.FileData("test.jpg", "image/jpeg", content1)
            val fileData2 = AsyncFileService.FileData("test.jpg", "image/jpeg", content2)

            // When & Then
            assertThat(fileData1).isNotEqualTo(fileData2)
        }
    }
}
