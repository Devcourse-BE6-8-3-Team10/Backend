package com.back.domain.files.files.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("CloudFileStorageService 테스트")
internal class CloudFileStorageServiceTest {

    @Mock
    private lateinit var gcsStorage: Storage

    @InjectMocks
    private lateinit var cloudFileStorageService: CloudFileStorageService

    private val bucketName = "test-bucket"
    private val maxFileSize = 10485760L // 10MB

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(cloudFileStorageService, "bucketName", bucketName)
        ReflectionTestUtils.setField(cloudFileStorageService, "maxFileSize", maxFileSize)
    }

    @Nested
    @DisplayName("파일 저장 테스트")
    internal inner class StoreFileTest {

        @Test
        @DisplayName("정상적인 파일 저장")
        fun t1() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            assertThat(result).endsWith(".jpg")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }

        @Test
        @DisplayName("파일 크기 초과 시 예외")
        fun t2() {
            // Given
            val largeFileContent = ByteArray(maxFileSize.toInt() + 1)
            val originalFilename = "large.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.storeFile(largeFileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("파일 크기가 너무 큽니다")

            verify(gcsStorage, never()).create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))
        }

        @Test
        @DisplayName("허용되지 않는 파일 형식 시 예외")
        fun t3() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.exe"
            val contentType = "application/x-msdownload"
            val subFolder = "post_1"

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("허용되지 않는 파일 형식입니다.")

            verify(gcsStorage, never()).create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))
        }

        @Test
        @DisplayName("subFolder 정규화 처리")
        fun t4() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "../../../post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }

        @Test
        @DisplayName("PDF 파일 저장")
        fun t5() {
            // Given
            val fileContent = "PDF content".toByteArray()
            val originalFilename = "document.pdf"
            val contentType = "application/pdf"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            assertThat(result).endsWith(".pdf")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }

        @Test
        @DisplayName("텍스트 파일 저장")
        fun t6() {
            // Given
            val fileContent = "text content".toByteArray()
            val originalFilename = "document.txt"
            val contentType = "text/plain"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            assertThat(result).endsWith(".txt")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }

        @Test
        @DisplayName("GCS 저장 실패 시 예외")
        fun t7() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java)))
                .willThrow(RuntimeException("GCS 저장 실패"))

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("클라우드 스토리지 파일 저장 중 오류가 발생했습니다.")
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    internal inner class DeletePhysicalFileTest {

        @Test
        @DisplayName("정상적인 파일 삭제")
        fun t8() {
            // Given
            val fileUrl = "https://storage.googleapis.com/$bucketName/post_1/test-uuid.jpg"
            given(gcsStorage.delete(ArgumentMatchers.any(BlobId::class.java))).willReturn(true)

            // When
            cloudFileStorageService.deletePhysicalFile(fileUrl)

            // Then
            verify(gcsStorage).delete(ArgumentMatchers.any(BlobId::class.java))
        }

        @Test
        @DisplayName("빈 URL로 삭제 시 처리 안함")
        fun t9() {
            // Given
            val fileUrl = ""

            // When
            cloudFileStorageService.deletePhysicalFile(fileUrl)

            // Then
            verify(gcsStorage, never()).delete(ArgumentMatchers.any(BlobId::class.java))
        }

        @Test
        @DisplayName("잘못된 URL 형식으로 삭제 시 처리 안함")
        fun t10() {
            // Given
            val fileUrl = "https://other-domain.com/file.jpg"

            // When
            cloudFileStorageService.deletePhysicalFile(fileUrl)

            // Then
            verify(gcsStorage, never()).delete(ArgumentMatchers.any(BlobId::class.java))
        }

        @Test
        @DisplayName("GCS 삭제 중 예외 발생")
        fun t12() {
            // Given
            val fileUrl = "https://storage.googleapis.com/$bucketName/post_1/test-uuid.jpg"
            given(gcsStorage.delete(ArgumentMatchers.any(BlobId::class.java))).willThrow(RuntimeException("GCS 삭제 실패"))

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.deletePhysicalFile(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("클라우드 스토리지 파일 삭제 중 오류가 발생했습니다.")
        }
    }

    @Nested
    @DisplayName("파일 리소스 로드 테스트")
    internal inner class LoadFileAsResourceTest {

        @Test
        @DisplayName("정상적인 파일 리소스 로드")
        fun t13() {
            // Given
            val fileUrl = "https://storage.googleapis.com/$bucketName/post_1/test-uuid.jpg"

            // When
            val resource = cloudFileStorageService.loadFileAsResource(fileUrl)

            // Then
            assertThat(resource).isNotNull
            assertThat(resource.url.toString()).isEqualTo(fileUrl)
        }

        @Test
        @DisplayName("잘못된 버킷 URL로 리소스 로드 시 예외")
        fun t14() {
            // Given
            val fileUrl = "https://storage.googleapis.com/other-bucket/post_1/test-uuid.jpg"

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("허용되지 않는 파일 URL입니다: $fileUrl")
        }

        @Test
        @DisplayName("GCS가 아닌 URL로 리소스 로드 시 예외")
        fun t15() {
            // Given
            val fileUrl = "https://other-domain.com/file.jpg"

            // When & Then
            assertThatThrownBy {
                cloudFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("허용되지 않는 파일 URL입니다: $fileUrl")
        }
    }

    @Nested
    @DisplayName("헬퍼 메서드 테스트")
    internal inner class HelperMethodTest {

        @Test
        @DisplayName("확장자 없는 파일명 처리")
        fun t16() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "testfile"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }

        @Test
        @DisplayName("다양한 이미지 형식 허용")
        fun t17() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.png"
            val contentType = "image/png"
            val subFolder = "post_1"

            given(gcsStorage.create(ArgumentMatchers.any(BlobInfo::class.java), ArgumentMatchers.any(ByteArray::class.java))).willReturn(null)

            // When
            val result = cloudFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/$bucketName/post_1/")
            assertThat(result).endsWith(".png")
            verify(gcsStorage).create(ArgumentMatchers.any(BlobInfo::class.java), eq(fileContent))
        }
    }
}
