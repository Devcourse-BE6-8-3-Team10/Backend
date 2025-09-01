package com.back.domain.files.files.controller

import com.back.domain.files.files.dto.FileUploadResponseDto
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.files.files.service.FilesService
import com.back.global.rsData.RsData
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminFilesController 테스트")
internal class AdminFilesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var filesService: FilesService

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Test
    @DisplayName("관리자 전체 파일 목록 조회 - 성공")
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun getAllFiles_success() {
        // given
        val mockFileList = listOf(
            FileUploadResponseDto(
                id = 1L,
                postId = 10L,
                fileName = "test.png",
                fileType = "image/png",
                fileSize = 2048L,
                fileUrl = "http://example.com/test.png",
                sortOrder = 1,
                createdAt = LocalDateTime.now()
            ),
            FileUploadResponseDto(
                id = 2L,
                postId = 10L,
                fileName = "doc.pdf",
                fileType = "application/pdf",
                fileSize = 4096L,
                fileUrl = "http://example.com/doc.pdf",
                sortOrder = 2,
                createdAt = LocalDateTime.now()
            )
        )
        val page: org.springframework.data.domain.Page<FileUploadResponseDto> = PageImpl(mockFileList, PageRequest.of(0, 10), mockFileList.size.toLong())
        val response = RsData("200", "파일 목록 조회 성공", page)

        whenever(filesService.adminGetAllFiles(any())).thenReturn(response)

        // when & then
        mockMvc.perform(get("/api/admin/files?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("파일 목록 조회 성공"))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].fileName").value("test.png"))
            .andExpect(jsonPath("$.data.content[1].fileName").value("doc.pdf"))
    }

    @Test
    @DisplayName("관리자 전체 파일 목록 조회 - 파일 없음(페이징)")
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun getAllFiles_whenEmpty() {
        // given
        val emptyPage: org.springframework.data.domain.Page<FileUploadResponseDto> = PageImpl(emptyList(), PageRequest.of(0, 2), 0)
        val response = RsData("200", "등록된 파일이 없습니다.", emptyPage)

        whenever(filesService.adminGetAllFiles(any())).thenReturn(response)

        // when & then
        mockMvc.perform(get("/api/admin/files?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록된 파일이 없습니다."))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    @DisplayName("관리자 파일 단일 조회 - 성공")
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun getFileById_success() {
        // given
        val mockDto = FileUploadResponseDto(
            id = 1L,
            postId = 10L,
            fileName = "test.png",
            fileType = "image/png",
            fileSize = 2048L,
            fileUrl = "http://example.com/test.png",
            sortOrder = 1,
            createdAt = LocalDateTime.now()
        )
        val response = RsData("200", "파일 조회 성공 (관리자)", mockDto)

        whenever(filesService.adminGetFileById(1L)).thenReturn(response)

        // when & then
        mockMvc.perform(get("/api/admin/files/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("파일 조회 성공 (관리자)"))
            .andExpect(jsonPath("$.data.fileName").value("test.png"))
            .andExpect(jsonPath("$.data.fileType").value("image/png"))
            .andExpect(jsonPath("$.data.fileSize").value(2048))
    }

    @Test
    @DisplayName("관리자 파일 삭제 - 성공")
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun deleteFile_success() {
        // given
        val fileId = 1L
        val response = RsData<Void?>("200", "파일 삭제 성공 (관리자)", null)

        whenever(filesService.adminDeleteFile(fileId)).thenReturn(response)

        // when & then
        mockMvc.perform(delete("/api/admin/files/{fileId}", fileId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("파일 삭제 성공 (관리자)"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }
}