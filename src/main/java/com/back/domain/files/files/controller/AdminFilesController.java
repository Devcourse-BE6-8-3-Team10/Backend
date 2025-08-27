package com.back.domain.files.files.controller;

import com.back.domain.files.files.dto.FileUploadResponseDto;
import com.back.domain.files.files.service.FilesService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // 관리자 권한
@Tag(name = "파일 관리(관리자)", description = "관리자 전용 파일 관리 API")
public class AdminFilesController {

    private final FilesService filesService;

    // 관리자용 파일 조회 API
    @GetMapping
    @Operation(summary = "모든 파일 조회", description = "관리자가 모든 파일을 조회합니다")
    public RsData<Page<FileUploadResponseDto>> getAllFiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return filesService.adminGetAllFiles(pageable);
    }

    // 관리자용 파일 단일 조회 API
    @GetMapping("/{fileId}")
    @Operation(summary = "파일 단일 조회", description = "관리자가 특정 파일을 조회합니다")
    public RsData<FileUploadResponseDto> getFileById(@PathVariable Long fileId) {
        return filesService.adminGetFileById(fileId);
    }

    // 관리자용 파일 삭제 API
    @DeleteMapping("/{fileId}")
    @Operation(summary = "파일 삭제", description = "관리자가 특정 파일을 삭제합니다")
    public RsData<Void> deleteFile(@PathVariable Long fileId) {
        return filesService.adminDeleteFile(fileId);
    }
}
