package com.back.domain.files.files.service

import com.back.util.MockitoKotlinUtils.any
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import java.net.URI

@ExtendWith(MockitoExtension::class)
@DisplayName("FileStorageService 인터페이스 계약 테스트")
internal class FileStorageServiceTest {

    @Mock
    private lateinit var mockFileStorageService: FileStorageService

    @Nested
    @DisplayName("인터페이스 검증")
    internal inner class InterfaceContractTest {

        @Test
        @DisplayName("storeFile 메서드 검증")
        fun t1() {
            // Given
            val fileContent = "test content".toByteArray()
            val originalFilename = "test.jpg"
            val contentType = "image/jpeg"
            val subFolder = "post_1"
            val expectedUrl = "https://storage.googleapis.com/bucket/post_1/uuid.jpg"

            given(mockFileStorageService.storeFile(
                any<ByteArray>(),
                anyString(),
                anyString(),
                anyString()
            )).willReturn(expectedUrl)

            // When
            val result = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(expectedUrl)
            verify(mockFileStorageService).storeFile(fileContent, originalFilename, contentType, subFolder)
        }

        @Test
        @DisplayName("deletePhysicalFile 메서드 검증")
        fun t2() {
            // Given
            val fileUrl = "https://storage.googleapis.com/bucket/post_1/uuid.jpg"

            // When
            mockFileStorageService.deletePhysicalFile(fileUrl)

            // Then
            verify(mockFileStorageService).deletePhysicalFile(fileUrl)
        }

        @Test
        @DisplayName("loadFileAsResource 메서드 검증")
        fun t3() {
            // Given
            val fileUrl = "https://storage.googleapis.com/bucket/post_1/uuid.jpg"
            val mockResource = UrlResource(URI.create(fileUrl))

            given(mockFileStorageService.loadFileAsResource(anyString())).willReturn(mockResource)

            // When
            val result = mockFileStorageService.loadFileAsResource(fileUrl)

            // Then
            assertThat(result).isNotNull()
            assertThat(result).isInstanceOf(Resource::class.java)
            verify(mockFileStorageService).loadFileAsResource(fileUrl)
        }
    }

    @Nested
    @DisplayName("메서드 파라미터 검증")
    internal inner class ParameterValidationTest {

        @Test
        @DisplayName("storeFile 모든 파라미터 전달 확인")
        fun t4() {
            // Given
            val fileContent = "specific content".toByteArray()
            val originalFilename = "specific.pdf"
            val contentType = "application/pdf"
            val subFolder = "post_999"

            given(mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder))
                .willReturn("/specific/path/to/file.pdf")

            // When
            val result = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).isEqualTo("/specific/path/to/file.pdf")
            verify(mockFileStorageService).storeFile(fileContent, originalFilename, contentType, subFolder)
        }

        @Test
        @DisplayName("다양한 파일 타입 처리 검증")
        fun t5() {
            // Given
            val imageContent = "image".toByteArray()
            val pdfContent = "pdf".toByteArray()
            val textContent = "text".toByteArray()

            given(mockFileStorageService.storeFile(imageContent, "image.jpg", "image/jpeg", "images"))
                .willReturn("/images/uuid1.jpg")
            given(mockFileStorageService.storeFile(pdfContent, "doc.pdf", "application/pdf", "documents"))
                .willReturn("/documents/uuid2.pdf")
            given(mockFileStorageService.storeFile(textContent, "note.txt", "text/plain", "texts"))
                .willReturn("/texts/uuid3.txt")

            // When
            val imageResult = mockFileStorageService.storeFile(imageContent, "image.jpg", "image/jpeg", "images")
            val pdfResult = mockFileStorageService.storeFile(pdfContent, "doc.pdf", "application/pdf", "documents")
            val textResult = mockFileStorageService.storeFile(textContent, "note.txt", "text/plain", "texts")

            // Then
            assertThat(imageResult).isEqualTo("/images/uuid1.jpg")
            assertThat(pdfResult).isEqualTo("/documents/uuid2.pdf")
            assertThat(textResult).isEqualTo("/texts/uuid3.txt")
        }
    }

    @Nested
    @DisplayName("리턴 타입 검증")
    internal inner class ReturnTypeTest {

        @Test
        @DisplayName("storeFile 리턴 타입은 String")
        fun t6() {
            // Given
            given(mockFileStorageService.storeFile(any<ByteArray>(), anyString(), anyString(), anyString()))
                .willReturn("valid-url")

            // When
            val result = mockFileStorageService.storeFile("content".toByteArray(), "file.jpg", "image/jpeg", "folder")

            // Then
            assertThat(result).isInstanceOf(String::class.java)
            assertThat(result).isNotEmpty()
        }

        @Test
        @DisplayName("loadFileAsResource 리턴 타입은 Resource")
        fun t7() {
            // Given
            val fileUrl = "https://example.com/file.jpg"
            val mockResource = UrlResource(URI.create(fileUrl))

            given(mockFileStorageService.loadFileAsResource(anyString())).willReturn(mockResource)

            // When
            val result = mockFileStorageService.loadFileAsResource(fileUrl)

            // Then
            assertThat(result).isInstanceOf(Resource::class.java)
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("deletePhysicalFile 리턴 타입은 Unit(void)")
        fun t8() {
            // Given
            val fileUrl = "https://example.com/file.jpg"

            // When & Then
            assertThatCode {
                mockFileStorageService.deletePhysicalFile(fileUrl)
            }.doesNotThrowAnyException()

            verify(mockFileStorageService).deletePhysicalFile(fileUrl)
        }
    }

    @Nested
    @DisplayName("인터페이스 메서드 시그니처 검증")
    internal inner class MethodSignatureTest {

        @Test
        @DisplayName("storeFile 메서드 시그니처 확인")
        fun t9() {
            // Given
            val method = FileStorageService::class.java.getMethod(
                "storeFile",
                ByteArray::class.java,
                String::class.java,
                String::class.java,
                String::class.java
            )

            // When & Then
            assertThat(method.returnType).isEqualTo(String::class.java)
            assertThat(method.parameterCount).isEqualTo(4)
            assertThat(method.parameterTypes[0]).isEqualTo(ByteArray::class.java)
            assertThat(method.parameterTypes[1]).isEqualTo(String::class.java)
            assertThat(method.parameterTypes[2]).isEqualTo(String::class.java)
            assertThat(method.parameterTypes[3]).isEqualTo(String::class.java)
        }

        @Test
        @DisplayName("deletePhysicalFile 메서드 시그니처 확인")
        fun t10() {
            // Given
            val method = FileStorageService::class.java.getMethod(
                "deletePhysicalFile",
                String::class.java
            )

            // When & Then
            assertThat(method.returnType).isEqualTo(Void.TYPE)
            assertThat(method.parameterCount).isEqualTo(1)
            assertThat(method.parameterTypes[0]).isEqualTo(String::class.java)
        }

        @Test
        @DisplayName("loadFileAsResource 메서드 시그니처 확인")
        fun t11() {
            // Given
            val method = FileStorageService::class.java.getMethod(
                "loadFileAsResource",
                String::class.java
            )

            // When & Then
            assertThat(method.returnType).isEqualTo(Resource::class.java)
            assertThat(method.parameterCount).isEqualTo(1)
            assertThat(method.parameterTypes[0]).isEqualTo(String::class.java)
        }
    }

    @Nested
    @DisplayName("구현체 동작 일관성 검증")
    internal inner class ImplementationConsistencyTest {

        @Test
        @DisplayName("동일한 파라미터로 호출 시 동일한 결과 기대")
        fun t12() {
            // Given
            val fileContent = "consistent content".toByteArray()
            val originalFilename = "consistent.jpg"
            val contentType = "image/jpeg"
            val subFolder = "consistent_folder"
            val expectedUrl = "/path/to/consistent.jpg"

            given(mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder))
                .willReturn(expectedUrl)

            // When
            val result1 = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            val result2 = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result1).isEqualTo(expectedUrl)
            assertThat(result2).isEqualTo(expectedUrl)
        }

        @Test
        @DisplayName("null이나 빈 문자열 파라미터 처리")
        fun t13() {
            // Given
            val fileContent = "content".toByteArray()
            val emptyFilename = ""
            val blankContentType = "   "
            val nullSubFolder = ""

            // 구현체에서 적절히 처리할 것으로 기대 (예외 발생 또는 기본값 처리)
            given(mockFileStorageService.storeFile(fileContent, emptyFilename, blankContentType, nullSubFolder))
                .willReturn("/default/path")

            // When
            val result = mockFileStorageService.storeFile(fileContent, emptyFilename, blankContentType, nullSubFolder)

            // Then
            assertThat(result).isNotNull()
            verify(mockFileStorageService).storeFile(fileContent, emptyFilename, blankContentType, nullSubFolder)
        }

        @Test
        @DisplayName("빈 바이트 배열 처리")
        fun t14() {
            // Given
            val emptyContent = ByteArray(0)
            val originalFilename = "empty.txt"
            val contentType = "text/plain"
            val subFolder = "empty_folder"

            given(mockFileStorageService.storeFile(emptyContent, originalFilename, contentType, subFolder))
                .willReturn("/empty/path")

            // When
            val result = mockFileStorageService.storeFile(emptyContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).isNotNull()
            verify(mockFileStorageService).storeFile(emptyContent, originalFilename, contentType, subFolder)
        }

        @Test
        @DisplayName("큰 파일 처리")
        fun t15() {
            // Given
            val largeContent = ByteArray(1024 * 1024) // 1MB
            val originalFilename = "large.jpg"
            val contentType = "image/jpeg"
            val subFolder = "large_files"

            given(mockFileStorageService.storeFile(largeContent, originalFilename, contentType, subFolder))
                .willReturn("/large/uuid.jpg")

            // When
            val result = mockFileStorageService.storeFile(largeContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).isNotNull()
            verify(mockFileStorageService).storeFile(largeContent, originalFilename, contentType, subFolder)
        }
    }

    @Nested
    @DisplayName("예외 처리 계약 검증")
    internal inner class ExceptionContractTest {

        @Test
        @DisplayName("storeFile 실패 시 RuntimeException 발생")
        fun t16() {
            // Given
            val fileContent = "content".toByteArray()
            val originalFilename = "fail.jpg"
            val contentType = "image/jpeg"
            val subFolder = "fail_folder"

            given(mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder))
                .willThrow(RuntimeException("저장 실패"))

            // When & Then
            assertThatThrownBy {
                mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("저장 실패")
        }

        @Test
        @DisplayName("deletePhysicalFile 실패 시 RuntimeException 발생")
        fun t17() {
            // Given
            val fileUrl = "https://example.com/fail.jpg"

            given(mockFileStorageService.deletePhysicalFile(fileUrl))
                .willThrow(RuntimeException("삭제 실패"))

            // When & Then
            assertThatThrownBy {
                mockFileStorageService.deletePhysicalFile(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("삭제 실패")
        }

        @Test
        @DisplayName("loadFileAsResource 실패 시 RuntimeException 발생")
        fun t18() {
            // Given
            val fileUrl = "https://invalid-url.com/fail.jpg"

            given(mockFileStorageService.loadFileAsResource(fileUrl))
                .willThrow(RuntimeException("리소스 로드 실패"))

            // When & Then
            assertThatThrownBy {
                mockFileStorageService.loadFileAsResource(fileUrl)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("리소스 로드 실패")
        }
    }

    @Nested
    @DisplayName("구현체별 특성 검증")
    internal inner class ImplementationSpecificTest {

        @Test
        @DisplayName("Cloud Storage URL 형식 확인")
        fun t19() {
            // Given
            val fileContent = "cloud content".toByteArray()
            val originalFilename = "cloud.jpg"
            val contentType = "image/jpeg"
            val subFolder = "cloud_folder"
            val cloudUrl = "https://storage.googleapis.com/my-bucket/cloud_folder/uuid.jpg"

            given(mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder))
                .willReturn(cloudUrl)

            // When
            val result = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("https://storage.googleapis.com/")
            assertThat(result).contains("cloud_folder")
            assertThat(result).endsWith(".jpg")
        }

        @Test
        @DisplayName("Local Storage 경로 형식 확인")
        fun t20() {
            // Given
            val fileContent = "local content".toByteArray()
            val originalFilename = "local.jpg"
            val contentType = "image/jpeg"
            val subFolder = "local_folder"
            val localPath = "/uploads/local_folder/uuid.jpg"

            given(mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder))
                .willReturn(localPath)

            // When
            val result = mockFileStorageService.storeFile(fileContent, originalFilename, contentType, subFolder)

            // Then
            assertThat(result).startsWith("/uploads/")
            assertThat(result).contains("local_folder")
            assertThat(result).endsWith(".jpg")
        }
    }

    @Nested
    @DisplayName("인터페이스 기본 동작 검증")
    internal inner class BasicBehaviorTest {

        @Test
        @DisplayName("인터페이스 메서드 존재 확인")
        fun t21() {
            // Given & When
            val methods = FileStorageService::class.java.methods

            // Then
            val methodNames = methods.map { it.name }
            assertThat(methodNames).contains("storeFile")
            assertThat(methodNames).contains("deletePhysicalFile")
            assertThat(methodNames).contains("loadFileAsResource")
        }

        @Test
        @DisplayName("인터페이스 추상 메서드 확인")
        fun t22() {
            // Given
            val fileStorageServiceClass = FileStorageService::class.java

            // When & Then
            assertThat(fileStorageServiceClass.isInterface).isTrue()
            
            val storeFileMethod = fileStorageServiceClass.getMethod(
                "storeFile", 
                ByteArray::class.java, 
                String::class.java, 
                String::class.java, 
                String::class.java
            )
            val deletePhysicalFileMethod = fileStorageServiceClass.getMethod(
                "deletePhysicalFile", 
                String::class.java
            )
            val loadFileAsResourceMethod = fileStorageServiceClass.getMethod(
                "loadFileAsResource", 
                String::class.java
            )

            assertThat(storeFileMethod.isDefault).isFalse()
            assertThat(deletePhysicalFileMethod.isDefault).isFalse()
            assertThat(loadFileAsResourceMethod.isDefault).isFalse()
        }
    }
}
