package com.back.domain.files.files.controller

import com.back.domain.files.files.service.FileStorageService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FileDownloadController 테스트")
internal class FileDownloadControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Test
    @DisplayName("이미지 파일 다운로드")
    fun downloadImageFile_inline() {
        // given
        val fileUrl = "/files/test/image.png"
        val fileContent = "mock image content".toByteArray()
        val mockResource: Resource = object : ByteArrayResource(fileContent) {
            override fun getFilename(): String = "image.png"
        }

        whenever(fileStorageService.loadFileAsResource(fileUrl)).thenReturn(mockResource)

        // when & then
        mockMvc.perform(get("/files/test/image.png"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
    }

    @Test
    @DisplayName("PDF 파일 다운로드")
    fun downloadPdfFile_inline() {
        // given
        val fileUrl = "/files/documents/test.pdf"
        val fileContent = "mock pdf content".toByteArray()
        val mockResource: Resource = object : ByteArrayResource(fileContent) {
            override fun getFilename(): String = "test.pdf"
        }

        whenever(fileStorageService.loadFileAsResource(fileUrl)).thenReturn(mockResource)

        // when & then
        mockMvc.perform(get("/files/documents/test.pdf"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
    }

    @Test
    @DisplayName("존재하지 않는 파일 다운로드 - 404 에러")
    fun downloadNonExistentFile_notFound() {
        // given
        val fileUrl = "/files/nonexistent/file.txt"
        
        whenever(fileStorageService.loadFileAsResource(fileUrl)).thenThrow(RuntimeException("File not found"))

        // when & then
        mockMvc.perform(get("/files/nonexistent/file.txt"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("잘못된 경로 접근 - 400 에러")
    fun downloadWithInvalidPath_badRequest() {
        // when & then - 상위 디렉토리 접근 시도
        mockMvc.perform(get("/files/../../../etc/passwd"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("한글 파일명 다운로드 - 정상 처리")
    fun downloadKoreanFilename_success() {
        // given
        val fileUrl = "/files/한글파일.txt"
        val fileContent = "한글 내용".toByteArray()
        val mockResource: Resource = object : ByteArrayResource(fileContent) {
            override fun getFilename(): String = "한글파일.txt"
        }

        whenever(fileStorageService.loadFileAsResource(fileUrl)).thenReturn(mockResource)

        // when & then
        mockMvc.perform(get("/files/한글파일.txt"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.TEXT_PLAIN))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
    }
}