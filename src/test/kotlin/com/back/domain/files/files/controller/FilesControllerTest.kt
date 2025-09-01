package com.back.domain.files.files.controller

import com.back.domain.files.files.dto.FileUploadResponseDto
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.files.files.service.FilesService
import com.back.global.rsData.RsData
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FilesController 테스트")
internal class FilesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var filesService: FilesService

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Nested
    @DisplayName("파일 업로드 API 테스트")
    internal inner class FileUploadTest {

        @Test
        @DisplayName("파일 업로드 성공 - 단일 파일")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun uploadFiles_singleFile_success() {
            // given
            val postId = 5L
            val file = MockMultipartFile(
                "files", 
                "test.png", 
                "image/png", 
                "fake-image-content".toByteArray()
            )
            
            val response = RsData(
                "200",
                "파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다.",
                "Upload initiated"
            )

            whenever(filesService.uploadFiles(eq(postId), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                multipart("/api/posts/{postId}/files", postId)
                    .file(file)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다."))
                .andExpect(jsonPath("$.data").value("Upload initiated"))
        }

        @Test
        @DisplayName("파일 업로드 성공 - 다중 파일")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun uploadFiles_multipleFiles_success() {
            // given
            val postId = 5L
            val file1 = MockMultipartFile("files", "test1.png", "image/png", "content1".toByteArray())
            val file2 = MockMultipartFile("files", "test2.jpg", "image/jpeg", "content2".toByteArray())
            
            val response = RsData(
                "200",
                "파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다.",
                "Upload initiated"
            )

            whenever(filesService.uploadFiles(eq(postId), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                multipart("/api/posts/{postId}/files", postId)
                    .file(file1)
                    .file(file2)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다."))
                .andExpect(jsonPath("$.data").value("Upload initiated"))
        }

        @Test
        @DisplayName("빈 파일 업로드 요청 - 성공")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun uploadFiles_emptyFile_success() {
            // given
            val postId = 5L
            val emptyFile = MockMultipartFile("files", "empty.txt", "text/plain", ByteArray(0))
            
            val response = RsData(
                "200",
                "파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다.",
                "Upload initiated"
            )

            whenever(filesService.uploadFiles(eq(postId), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                multipart("/api/posts/{postId}/files", postId)
                    .file(emptyFile)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다."))
                .andExpect(jsonPath("$.data").value("Upload initiated"))
        }
    }

    @Nested
    @DisplayName("파일 조회 API 테스트")
    internal inner class FileRetrievalTest {

        @Test
        @DisplayName("게시글의 파일 조회 성공 - 파일 있음")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun getFilesByPostId_withFiles_success() {
            // given
            val postId = 5L
            val fileList = listOf(
                FileUploadResponseDto(
                    id = 1L,
                    postId = postId,
                    fileName = "test1.png",
                    fileType = "image/png",
                    fileSize = 2048L,
                    fileUrl = "http://example.com/uploads/test1.png",
                    sortOrder = 1,
                    createdAt = LocalDateTime.now()
                ),
                FileUploadResponseDto(
                    id = 2L,
                    postId = postId,
                    fileName = "test2.jpg",
                    fileType = "image/jpeg",
                    fileSize = 3072L,
                    fileUrl = "http://example.com/uploads/test2.jpg",
                    sortOrder = 2,
                    createdAt = LocalDateTime.now()
                )
            )

            val response = RsData("200", "파일 목록 조회 성공", fileList)

            whenever(filesService.getFilesByPostId(postId)).thenReturn(response)

            // when & then
            mockMvc.perform(get("/api/posts/{postId}/files", postId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].fileName").value("test1.png"))
                .andExpect(jsonPath("$.data[1].fileName").value("test2.jpg"))
                .andExpect(jsonPath("$.data[0].fileType").value("image/png"))
                .andExpect(jsonPath("$.data[1].fileType").value("image/jpeg"))
        }

        @Test
        @DisplayName("게시글의 파일 조회 - 파일 없음")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun getFilesByPostId_noFiles_success() {
            // given
            val postId = 5L
            val response = RsData("200", "첨부된 파일이 없습니다.", emptyList<FileUploadResponseDto>())

            whenever(filesService.getFilesByPostId(postId)).thenReturn(response)

            // when & then
            mockMvc.perform(get("/api/posts/{postId}/files", postId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("첨부된 파일이 없습니다."))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("파일 삭제 API 테스트")
    internal inner class FileDeletionTest {

        @Test
        @DisplayName("파일 삭제 성공")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun deleteFile_success() {
            // given
            val postId = 5L
            val fileId = 1L
            val response = RsData<Void?>("200", "파일 삭제 성공", null)

            whenever(filesService.deleteFile(postId, fileId)).thenReturn(response)

            // when & then
            mockMvc.perform(delete("/api/posts/{postId}/files/{fileId}", postId, fileId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        @DisplayName("파일 삭제 - 여러 파일 중 하나 삭제")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun deleteFile_fromMultipleFiles_success() {
            // given
            val postId = 10L
            val fileId = 3L
            val response = RsData<Void?>("200", "파일 삭제 성공", null)

            whenever(filesService.deleteFile(postId, fileId)).thenReturn(response)

            // when & then
            mockMvc.perform(delete("/api/posts/{postId}/files/{fileId}", postId, fileId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }

    @Nested
    @DisplayName("경로 파라미터 검증 테스트")
    internal inner class PathParameterValidationTest {

        @Test
        @DisplayName("양수가 아닌 postId로 파일 업로드 시도")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun uploadFiles_invalidPostId_badRequest() {
            // given
            val invalidPostId = 0L
            val file = MockMultipartFile("files", "test.png", "image/png", "content".toByteArray())

            // when & then
            mockMvc.perform(
                multipart("/api/posts/{postId}/files", invalidPostId)
                    .file(file)
            )
                .andExpect(status().isBadRequest) // @Positive 검증 실패
        }

        @Test
        @DisplayName("양수가 아닌 fileId로 파일 삭제 시도")
        @WithMockUser(username = "test-user", roles = ["USER"])
        fun deleteFile_invalidFileId_badRequest() {
            // given
            val postId = 5L
            val invalidFileId = -1L

            // when & then
            mockMvc.perform(delete("/api/posts/{postId}/files/{fileId}", postId, invalidFileId))
                .andExpect(status().isBadRequest) // @Positive 검증 실패
        }
    }

    @Nested
    @DisplayName("인증 및 권한 테스트")
    internal inner class AuthenticationTest {

        @Test
        @DisplayName("로그인하지 않은 상태에서 파일 업로드 시도")
        fun uploadFiles_unauthenticated_forbidden() {
            // given
            val postId = 5L
            val file = MockMultipartFile("files", "test.png", "image/png", "content".toByteArray())

            // when & then
            mockMvc.perform(
                multipart("/api/posts/{postId}/files", postId)
                    .file(file)
            )
                .andExpect(status().isForbidden)
        }

        @Test
        @DisplayName("로그인하지 않은 상태에서 파일 삭제 시도")
        fun deleteFile_unauthenticated_forbidden() {
            // given
            val postId = 5L
            val fileId = 1L

            // when & then
            mockMvc.perform(delete("/api/posts/{postId}/files/{fileId}", postId, fileId))
                .andExpect(status().isForbidden)
        }
    }
}