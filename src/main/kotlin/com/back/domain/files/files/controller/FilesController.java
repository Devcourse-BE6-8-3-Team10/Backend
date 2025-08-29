package com.back.domain.files.files.controller;

import com.back.domain.files.files.dto.FileUploadResponseDto;
import com.back.domain.files.files.service.FilesService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "파일 관리(회원)", description = "게시글에 첨부된 파일 관리 API")
public class FilesController {

    private final FilesService filesService;

    @Operation(summary = "파일 업로드", description = "게시글에 파일을 업로드 합니다")
    @PostMapping(value = "/{postId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // uploadFiles 메서드의 반환 타입을 RsData<String>으로 변경
    public RsData<String> uploadFiles(
            @PathVariable @Positive long postId,
            @Parameter(description = "업로드할 파일들", required = false)
            @RequestPart(value = "files", required = false) MultipartFile[] files
    ) {
        // 서비스 호출 결과(즉각적인 응답)를 그대로 반환
        return filesService.uploadFiles(postId, files);
    }

    // 파일 조회
    @Operation(summary = "파일 조회", description = "게시글의 파일을 조회합니다")
    @GetMapping("/{postId}/files")
    public RsData<List<FileUploadResponseDto>> getFilesByPostId(@PathVariable Long postId) {
        return filesService.getFilesByPostId(postId);
    }

    // 파일 삭제
    @Operation(summary = "파일 삭제", description = "회원은 본인이 업로드한 파일을 삭제할 수 있습니다")
    @DeleteMapping("/{postId}/files/{fileId}")
    public RsData<Void> deleteFile(
            @PathVariable @Positive long postId,
            @PathVariable @Positive long fileId
    ) {
        return filesService.deleteFile(postId, fileId);
    }
}
