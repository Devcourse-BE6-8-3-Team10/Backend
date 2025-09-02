package com.back.domain.files.files.service

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
@DisplayName("LocalFileStorageService 테스트")
internal class LocalFileStorageServiceTest {

    @InjectMocks
    private lateinit var localFileStorageService: LocalFileStorageService

    @TempDir
    private lateinit var tempDir: Path

    private val maxFileSize = 10485760L // 10MB

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(localFileStorageService, "uploadDir", tempDir.toString())
        ReflectionTestUtils.setField(localFileStorageService, "maxFileSize", maxFileSize)
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

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/post_1/")
            assertThat(result).endsWith(".jpg")
            
            // 실제 파일이 생성되었는지 확인
            val relativePath = result.substring("/files/".length)
            val filePath = tempDir.resolve(relativePath)
            assertThat(Files.exists(filePath)).isTrue()
            assertThat(Files.readAllBytes(filePath)).isEqualTo(fileContent)
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
                localFileStorageService.storeFile(largeFileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("파일 크기가 너무 큽니다")
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
                localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("허용되지 않는 파일 형식입니다.")
        }

        @Test
        @DisplayName("서브폴더 자동 생성")
        fun t4() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "nested/deep/folder"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/nested/deep/folder/")
            assertThat(result).endsWith(".jpg")
            
            // 중첩 폴더가 생성되었는지 확인
            val folderPath = tempDir.resolve("nested/deep/folder")
            assertThat(Files.exists(folderPath)).isTrue()
            assertThat(Files.isDirectory(folderPath)).isTrue()
        }

        @Test
        @DisplayName("PDF 파일 저장")
        fun t5() {
            // Given
            val fileContent = "PDF content".toByteArray()
            val originalFilename = "document.pdf"
            val contentType = "application/pdf"
            val subFolder = "documents"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/documents/")
            assertThat(result).endsWith(".pdf")
            
            val relativePath = result.substring("/files/".length)
            val filePath = tempDir.resolve(relativePath)
            assertThat(Files.exists(filePath)).isTrue()
        }

        @Test
        @DisplayName("텍스트 파일 저장")
        fun t6() {
            // Given
            val fileContent = "text content".toByteArray()
            val originalFilename = "document.txt"
            val contentType = "text/plain"
            val subFolder = "texts"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/texts/")
            assertThat(result).endsWith(".txt")
        }

        @Test
        @DisplayName("확장자 없는 파일명 처리")
        fun t7() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "testfile"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/post_1/")
            // 확장자가 없으므로 UUID만 있음
            assertThat(result).doesNotContain(".")
        }

        @Test
        @DisplayName("빈 파일 저장")
        fun t8() {
            // Given
            val fileContent = ByteArray(0)
            val originalFilename = "empty.txt"
            val contentType = "text/plain"
            val subFolder = "empty"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/files/empty/")
            assertThat(result).endsWith(".txt")
            
            val relativePath = result.substring("/files/".length)
            val filePath = tempDir.resolve(relativePath)
            assertThat(Files.exists(filePath)).isTrue()
            assertThat(Files.size(filePath)).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    internal inner class DeletePhysicalFileTest {

        @Test
        @DisplayName("정상적인 파일 삭제 및 빈 부모 폴더 삭제")
        fun t9() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // 먼저 파일을 저장
            val fileUrl = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val relativePath = fileUrl.substring("/files/".length)
            val filePath = tempDir.resolve(relativePath)
            val parentDir = filePath.parent
            
            assertThat(Files.exists(filePath)).isTrue()
            assertThat(Files.exists(parentDir)).isTrue()

            // When
            localFileStorageService.deletePhysicalFile(fileUrl)

            // Then
            assertThat(Files.exists(filePath)).isFalse()
            assertThat(Files.exists(parentDir)).isFalse() // 부모 폴더도 삭제되었는지 확인
        }

        @Test
        @DisplayName("빈 URL로 삭제 시 처리 안함")
        fun t10() {
            // Given
            val fileUrl = ""

            // When & Then
            assertThatCode {
                localFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("공백 URL로 삭제 시 처리 안함")
        fun t11() {
            // Given
            val fileUrl = "   "

            // When & Then
            assertThatCode {
                localFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("존재하지 않는 파일 삭제 시 예외 대신 경고 로그")
        fun t12() {
            // Given
            val fileUrl = "/files/post_1/nonexistent.jpg"

            // When & Then
            assertThatCode {
                localFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("잘못된 URL 형식이어도 예외 없음")
        fun t13() {
            // Given
            val fileUrl = "invalid-url-format"

            // When & Then
            // 잘못된 형식의 URL도 결국 존재하지 않는 파일 경로로 취급되므로 예외가 발생하지 않음
            assertThatCode {
                localFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("파일 리소스 로드 테스트")
    internal inner class LoadFileAsResourceTest {

        @Test
        @DisplayName("정상적인 파일 리소스 로드")
        fun t14() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // 먼저 파일을 저장
            val fileUrl = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // When
            val resource = localFileStorageService.loadFileAsResource(fileUrl)

            // Then
            assertThat(resource).isNotNull()
            assertThat(resource.exists()).isTrue()
            assertThat(resource.isReadable).isTrue()
            assertThat(resource.contentLength()).isEqualTo(fileContent.size.toLong())
        }

        @Test
        @DisplayName("존재하지 않는 파일 리소스 로드 시 예외")
        fun t15() {
            // Given
            val fileUrl = "/files/post_1/nonexistent.jpg"

            // When & Then
            assertThatThrownBy {
                localFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("파일을 찾을 수 없거나 읽을 수 없습니다: $fileUrl")
        }

        @Test
        @DisplayName("잘못된 URL 형식으로 리소스 로드 시 예외")
        fun t16() {
            // Given
            val fileUrl = "invalid-url-format"

            // When & Then
            assertThatThrownBy {
                localFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
        }
    }

    @Nested
    @DisplayName("헬퍼 메서드 테스트")
    internal inner class HelperMethodTest {

        @Test
        @DisplayName("다양한 이미지 형식 허용 검증")
        fun t17() {
            // Given
            val fileContent = "image content".toByteArray()
            val subFolder = "images"

            val jpegFile = Pair("test.jpg", "image/jpeg")
            val pngFile = Pair("test.png", "image/png")
            val gifFile = Pair("test.gif", "image/gif")

            // When & Then
            assertThatCode {
                localFileStorageService.storeFile(fileContent, jpegFile.first, jpegFile.second, subFolder)
                localFileStorageService.storeFile(fileContent, pngFile.first, pngFile.second, subFolder)
                localFileStorageService.storeFile(fileContent, gifFile.first, gifFile.second, subFolder)
            }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("UUID 기반 파일명 생성 확인")
        fun t19() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "original.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // When
            val result1 = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val result2 = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result1).isNotEqualTo(result2) // UUID로 인해 다른 파일명
            assertThat(result1).endsWith(".jpg")
            assertThat(result2).endsWith(".jpg")
        }

        @Test
        @DisplayName("긴 확장자 처리")
        fun t20() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpeg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).endsWith(".jpeg")
        }

        @Test
        @DisplayName("파일명에 점이 여러 개 있는 경우")
        fun t21() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.file.name.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            // When
            val result = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).endsWith(".jpg") // 마지막 확장자만 사용
        }
    }

    @Nested
    @DisplayName("통합 워크플로우 테스트")
    internal inner class IntegrationWorkflowTest {

        @Test
        @DisplayName("저장 → 로드 → 삭제 전체 워크플로우")
        fun t22() {
            // Given
            val fileContent = "integration test content".toByteArray()
            val originalFilename = "integration.jpg"
            val contentType = "image/jpeg"
            val subFolder = "integration_test"

            // When - 저장
            val fileUrl = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val relativePath = fileUrl.substring("/files/".length)
            val filePath = tempDir.resolve(relativePath)
            val parentDir = filePath.parent

            // Then - 저장 확인
            assertThat(fileUrl).startsWith("/files/integration_test/")
            
            // When - 로드
            val resource = localFileStorageService.loadFileAsResource(fileUrl)

            // Then - 로드 확인
            assertThat(resource.exists()).isTrue()
            assertThat(resource.isReadable).isTrue()

            // When - 삭제
            localFileStorageService.deletePhysicalFile(fileUrl)

            // Then - 삭제 확인
            assertThat(Files.exists(filePath)).isFalse()
            assertThat(Files.exists(parentDir)).isFalse() // 부모 폴더도 삭제되었는지 확인
        }

        @Test
        @DisplayName("여러 파일 동시 저장 및 관리")
        fun t23() {
            // Given
            val fileContents = listOf(
                "content1".toByteArray(),
                "content2".toByteArray(),
                "content3".toByteArray()
            )
            val filenames = listOf("file1.jpg", "file2.png", "file3.pdf")
            val contentTypes = listOf("image/jpeg", "image/png", "application/pdf")
            val subFolder = "multi_files"

            // When
            val fileUrls = mutableListOf<String>()
            for (i in fileContents.indices) {
                val url = localFileStorageService.storeFile(
                    fileContents[i], 
                    filenames[i], 
                    contentTypes[i], 
                    subFolder
                )
                fileUrls.add(url)
            }

            // Then
            assertThat(fileUrls).hasSize(3)
            assertThat(fileUrls[0]).endsWith(".jpg")
            assertThat(fileUrls[1]).endsWith(".png")
            assertThat(fileUrls[2]).endsWith(".pdf")

            // 모든 파일이 실제로 저장되었는지 확인
            for (fileUrl in fileUrls) {
                val relativePath = fileUrl.substring("/files/".length)
                val filePath = tempDir.resolve(relativePath)
                assertThat(Files.exists(filePath)).isTrue()
            }
        }

        @Test
        @DisplayName("동일 폴더에 여러 파일 저장 시 충돌 없음")
        fun t24() {
            // Given
            val fileContent = "same content".toByteArray()
            val originalFilename = "same.jpg"
            val contentType = "image/jpeg"
            val subFolder = "same_folder"

            // When
            val url1 = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val url2 = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val url3 = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(url1).isNotEqualTo(url2)
            assertThat(url2).isNotEqualTo(url3)
            assertThat(url1).isNotEqualTo(url3)

            // 모든 파일이 실제로 저장되었는지 확인
            val relativePath1 = url1.substring("/files/".length)
            val relativePath2 = url2.substring("/files/".length)
            val relativePath3 = url3.substring("/files/".length)
            
            assertThat(Files.exists(tempDir.resolve(relativePath1))).isTrue()
            assertThat(Files.exists(tempDir.resolve(relativePath2))).isTrue()
            assertThat(Files.exists(tempDir.resolve(relativePath3))).isTrue()
        }
    }

    @Nested
    @DisplayName("에러 복구 테스트")
    internal inner class ErrorRecoveryTest {

        @Test
        @DisplayName("삭제 후 다시 로드 시 예외")
        fun t25() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            val fileUrl = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            localFileStorageService.deletePhysicalFile(fileUrl)

            // When & Then
            assertThatThrownBy {
                localFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("파일을 찾을 수 없거나 읽을 수 없습니다: $fileUrl")
        }

        @Test
        @DisplayName("이미 삭제된 파일 재삭제 시 예외 대신 경고 로그")
        fun t26() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"

            val fileUrl = localFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            localFileStorageService.deletePhysicalFile(fileUrl) // 첫 번째 삭제

            // When & Then
            // 다시 삭제를 시도해도 예외가 발생하지 않아야 함
            assertThatCode {
                localFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()
        }
    }
}
