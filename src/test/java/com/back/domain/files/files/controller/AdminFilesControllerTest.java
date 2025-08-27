package com.back.domain.files.files.controller;

import com.back.domain.files.files.dto.FileUploadResponseDto;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.files.files.service.FilesService;
import com.back.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFilesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilesService filesService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("관리자 전체 파일 목록 조회 - 성공")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllFiles_success() throws Exception {
        // given
        List<FileUploadResponseDto> mockFileList = List.of(
                new FileUploadResponseDto(1L, 10L, "test.png", "image/png", 2048L, "http://example.com/test.png", 1, LocalDateTime.now()),
                new FileUploadResponseDto(2L, 10L, "doc.pdf", "application/pdf", 4096L, "http://example.com/doc.pdf", 2, LocalDateTime.now())
        );
        Page<FileUploadResponseDto> page = new PageImpl(mockFileList, PageRequest.of(0, 10), mockFileList.size());
        RsData<Page<FileUploadResponseDto>> response = new RsData<>("200", "파일 목록 조회 성공", page);

        Mockito.when(filesService.adminGetAllFiles(Mockito.any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/files?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].fileName").value("test.png"))
                .andExpect(jsonPath("$.data.content[1].fileName").value("doc.pdf"));
    }

    @Test
    @DisplayName("관리자 전체 파일 목록 조회 - 파일 없음(페이징)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllFiles_whenEmpty() throws Exception {
        // given
        Page<FileUploadResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 2), 0);
        RsData<Page<FileUploadResponseDto>> response = new RsData<>("200", "등록된 파일이 없습니다.", emptyPage);

        Mockito.when(filesService.adminGetAllFiles(Mockito.any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/files?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("등록된 파일이 없습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("관리자 파일 단일 조회 - 성공")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getFileById_success() throws Exception {
        // given
        FileUploadResponseDto mockDto = new FileUploadResponseDto(
                1L, 10L, "test.png", "image/png", 2048L, "http://example.com/test.png", 1, LocalDateTime.now()
        );
        RsData<FileUploadResponseDto> response = new RsData<>("200", "파일 조회 성공", mockDto);

        Mockito.when(filesService.adminGetFileById(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 조회 성공"))
                .andExpect(jsonPath("$.data.fileName").value("test.png"))
                .andExpect(jsonPath("$.data.fileType").value("image/png"))
                .andExpect(jsonPath("$.data.fileSize").value(2048));
    }

    @Test
    @DisplayName("관리자 파일 삭제 - 성공")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteFile_success() throws Exception {
        // given
        Long fileId = 1L;
        RsData<Void> response = new RsData<>("200", "파일 삭제 성공", null);

        Mockito.when(filesService.adminDeleteFile(fileId)).thenReturn(response);

        // when & then
        mockMvc.perform(delete("/api/admin/files/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("파일 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}