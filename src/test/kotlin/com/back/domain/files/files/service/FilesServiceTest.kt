package com.back.domain.files.files.service

import com.back.domain.files.files.entity.Files
import com.back.domain.files.files.repository.FilesRepository
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.util.MockitoKotlinUtils.any
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("FilesService 테스트")
internal class FilesServiceTest {

    @Mock
    private lateinit var filesRepository: FilesRepository

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var rq: Rq

    @Mock
    private lateinit var asyncFileService: AsyncFileService

    @InjectMocks
    private lateinit var filesService: FilesService

    private lateinit var testMember: Member
    private lateinit var otherMember: Member
    private lateinit var adminMember: Member
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

        // 다른 멤버 생성
        otherMember = Member(
            email = "other@test.com",
            password = "password",
            name = "다른사용자",
            profileUrl = null,
            role = Role.USER,
            status = Status.ACTIVE
        )
        ReflectionTestUtils.setField(otherMember, "id", 2L)

        // 관리자 멤버 생성
        adminMember = Member(
            email = "admin@test.com",
            password = "password",
            name = "관리자",
            profileUrl = null,
            role = Role.ADMIN,
            status = Status.ACTIVE
        )
        ReflectionTestUtils.setField(testMember, "id", 1L)

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
        ReflectionTestUtils.setField(testFile, "createdAt", LocalDateTime.now())
    }

    @Nested
    @DisplayName("파일 업로드 테스트")
    internal inner class UploadFilesTest {

        @Test
        @DisplayName("정상적인 파일 업로드")
        fun t1() {
            // Given
            val postId = 1L
            val mockFile = MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".toByteArray()
            )
            val files = arrayOf<MultipartFile>(mockFile)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).contains("파일 업로드가 시작되었습니다")
            verify(asyncFileService).uploadFilesAsync(eq(postId), any<List<AsyncFileService.FileData>>())
        }

        @Test
        @DisplayName("존재하지 않는 게시글로 파일 업로드 시 예외")
        fun t2() {
            // Given
            val postId = 999L
            val mockFile = MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".toByteArray()
            )
            val files = arrayOf<MultipartFile>(mockFile)

            given(postRepository.findById(postId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                filesService.uploadFiles(postId, files)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404 : 존재하지 않는 게시글입니다: $postId")
        }

        @Test
        @DisplayName("게시글 작성자가 아닌 사용자의 파일 업로드 시 예외")
        fun t3() {
            // Given
            val postId = 1L
            val mockFile = MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".toByteArray()
            )
            val files = arrayOf<MultipartFile>(mockFile)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(2L) // 다른 사용자

            // When & Then
            assertThatThrownBy {
                filesService.uploadFiles(postId, files)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403 : 게시글 작성자만 파일을 업로드할 수 있습니다.")
        }

        @Test
        @DisplayName("업로드할 파일이 없는 경우")
        fun t4() {
            // Given
            val postId = 1L
            val files: Array<MultipartFile>? = null

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("업로드할 파일이 없습니다.")
            assertThat(result.data).isEqualTo("No files to upload")
        }

        @Test
        @DisplayName("빈 파일 배열 업로드")
        fun t5() {
            // Given
            val postId = 1L
            val files = arrayOf<MultipartFile>()

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("업로드할 파일이 없습니다.")
        }

        @Test
        @DisplayName("빈 파일과 정상 파일 혼합 업로드")
        fun t6() {
            // Given
            val postId = 1L
            val emptyFile = MockMultipartFile("empty", "", "image/jpeg", ByteArray(0))
            val normalFile = MockMultipartFile("normal", "test.jpg", "image/jpeg", "content".toByteArray())
            val files = arrayOf<MultipartFile>(emptyFile, normalFile)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            verify(asyncFileService).uploadFilesAsync(eq(postId), any<List<AsyncFileService.FileData>>())
        }
    }

    @Nested
    @DisplayName("파일 조회 테스트")
    internal inner class GetFilesByPostIdTest {

        @Test
        @DisplayName("게시글의 파일 목록 조회 성공")
        fun t7() {
            // Given
            val postId = 1L
            val files = listOf(testFile)

            given(filesRepository.findWithPostByPostId(postId)).willReturn(files)

            // When
            val result = filesService.getFilesByPostId(postId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 목록 조회 성공")
            assertThat(result.data).hasSize(1)
            assertThat(result.data!![0].fileName).isEqualTo("test.jpg")
        }

        @Test
        @DisplayName("파일이 없는 게시글 조회")
        fun t8() {
            // Given
            val postId = 1L
            val files = emptyList<Files>()

            given(filesRepository.findWithPostByPostId(postId)).willReturn(files)

            // When
            val result = filesService.getFilesByPostId(postId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("첨부된 파일이 없습니다.")
            assertThat(result.data).isEmpty()
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    internal inner class DeleteFileTest {

        @Test
        @DisplayName("정상적인 파일 삭제")
        fun t9() {
            // Given
            val postId = 1L
            val fileId = 1L

            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.deleteFile(postId, fileId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 삭제 성공")
            verify(fileStorageService).deletePhysicalFile("/uploads/test.jpg")
            verify(filesRepository).deleteById(fileId)
        }

        @Test
        @DisplayName("존재하지 않는 파일 삭제 시 예외")
        fun t10() {
            // Given
            val postId = 1L
            val fileId = 999L

            given(filesRepository.findById(fileId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                filesService.deleteFile(postId, fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404 : 파일이 존재하지 않습니다: $fileId")
        }

        @Test
        @DisplayName("다른 게시글의 파일 삭제 시 예외")
        fun t11() {
            // Given
            val postId = 2L // 다른 게시글 ID
            val fileId = 1L

            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))

            // When & Then
            assertThatThrownBy {
                filesService.deleteFile(postId, fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("400 : 해당 게시글에 속하지 않는 파일입니다: $fileId")
        }

        @Test
        @DisplayName("파일 작성자가 아닌 사용자의 삭제 시 예외")
        fun t12() {
            // Given
            val postId = 1L
            val fileId = 1L

            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))
            given(rq.memberId).willReturn(2L) // 다른 사용자

            // When & Then
            assertThatThrownBy {
                filesService.deleteFile(postId, fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403 : 해당 파일을 삭제할 권한이 없습니다.")
        }

        @Test
        @DisplayName("물리 파일 삭제 실패해도 논리 삭제는 진행")
        fun t13() {
            // Given
            val postId = 1L
            val fileId = 1L

            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))
            given(rq.memberId).willReturn(1L)
            doThrow(RuntimeException("물리 파일 삭제 실패")).`when`(fileStorageService).deletePhysicalFile(anyString())

            // When
            val result = filesService.deleteFile(postId, fileId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 삭제 성공")
            verify(filesRepository).deleteById(fileId)
        }
    }

    @Nested
    @DisplayName("관리자 전용 기능 테스트")
    internal inner class AdminFunctionTest {

        @Test
        @DisplayName("관리자 전체 파일 목록 조회 성공")
        fun t14() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val files = listOf(testFile)
            val filesPage = PageImpl(files, pageable, 1)

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findAll(pageable)).willReturn(filesPage)

            // When
            val result = filesService.adminGetAllFiles(pageable)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 목록 조회 성공")
            assertThat(result.data!!.content).hasSize(1)
        }

        @Test
        @DisplayName("관리자가 아닌 사용자의 전체 파일 조회 시 예외")
        fun t15() {
            // Given
            val pageable = PageRequest.of(0, 10)
            given(rq.isAdmin).willReturn(false)

            // When & Then
            assertThatThrownBy {
                filesService.adminGetAllFiles(pageable)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403 : 관리자 권한이 필요합니다.")
        }

        @Test
        @DisplayName("관리자 파일 없음 조회")
        fun t16() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val emptyPage = PageImpl<Files>(emptyList(), pageable, 0)

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findAll(pageable)).willReturn(emptyPage)

            // When
            val result = filesService.adminGetAllFiles(pageable)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("등록된 파일이 없습니다.")
            assertThat(result.data!!.content).isEmpty()
        }

        @Test
        @DisplayName("관리자 파일 개별 조회 성공")
        fun t17() {
            // Given
            val fileId = 1L

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))

            // When
            val result = filesService.adminGetFileById(fileId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 조회 성공 (관리자)")
            assertThat(result.data!!.fileName).isEqualTo("test.jpg")
        }

        @Test
        @DisplayName("관리자가 아닌 사용자의 파일 개별 조회 시 예외")
        fun t18() {
            // Given
            val fileId = 1L
            given(rq.isAdmin).willReturn(false)

            // When & Then
            assertThatThrownBy {
                filesService.adminGetFileById(fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403 : 관리자 권한이 필요합니다.")
        }

        @Test
        @DisplayName("관리자 존재하지 않는 파일 조회 시 예외")
        fun t19() {
            // Given
            val fileId = 999L

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findById(fileId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                filesService.adminGetFileById(fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404 : 파일이 존재하지 않습니다: $fileId")
        }

        @Test
        @DisplayName("관리자 파일 삭제 성공")
        fun t20() {
            // Given
            val fileId = 1L

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findById(fileId)).willReturn(Optional.of(testFile))

            // When
            val result = filesService.adminDeleteFile(fileId)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            assertThat(result.msg).isEqualTo("파일 삭제 성공 (관리자)")
            verify(fileStorageService).deletePhysicalFile("/uploads/test.jpg")
            verify(filesRepository).deleteById(fileId)
        }

        @Test
        @DisplayName("관리자가 아닌 사용자의 파일 삭제 시 예외")
        fun t21() {
            // Given
            val fileId = 1L
            given(rq.isAdmin).willReturn(false)

            // When & Then
            assertThatThrownBy {
                filesService.adminDeleteFile(fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("403 : 관리자 권한이 필요합니다.")
        }

        @Test
        @DisplayName("관리자 존재하지 않는 파일 삭제 시 예외")
        fun t22() {
            // Given
            val fileId = 999L

            given(rq.isAdmin).willReturn(true)
            given(filesRepository.findById(fileId)).willReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                filesService.adminDeleteFile(fileId)
            }
                .isInstanceOf(ServiceException::class.java)
                .hasMessage("404 : 파일이 존재하지 않습니다. $fileId")
        }
    }

    @Nested
    @DisplayName("헬퍼 메서드 테스트")
    internal inner class HelperMethodTest {

        @Test
        @DisplayName("파일명이 null인 파일 처리")
        fun t23() {
            // Given
            val postId = 1L
            val mockFile = MockMultipartFile("file", null, "image/jpeg", "content".toByteArray())
            val files = arrayOf<MultipartFile>(mockFile)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            verify(asyncFileService).uploadFilesAsync(eq(postId), any<List<AsyncFileService.FileData>>())
        }

        @Test
        @DisplayName("ContentType이 null인 파일 처리")
        fun t24() {
            // Given
            val postId = 1L
            val mockFile = MockMultipartFile("file", "test.jpg", null, "content".toByteArray())
            val files = arrayOf<MultipartFile>(mockFile)

            given(postRepository.findById(postId)).willReturn(Optional.of(testPost))
            given(rq.memberId).willReturn(1L)

            // When
            val result = filesService.uploadFiles(postId, files)

            // Then
            assertThat(result.resultCode).isEqualTo("200")
            verify(asyncFileService).uploadFilesAsync(eq(postId), any<List<AsyncFileService.FileData>>())
        }
    }
}
