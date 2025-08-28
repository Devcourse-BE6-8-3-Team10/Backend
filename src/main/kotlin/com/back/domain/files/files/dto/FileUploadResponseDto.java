package com.back.domain.files.files.dto;

import com.back.domain.files.files.entity.Files;

import java.time.LocalDateTime;

public record FileUploadResponseDto (
    // 파일 업로드 응답 DTO
     Long id,
     Long postId,
     String fileName,
     String fileType,
     long fileSize,
     String fileUrl,
     int sortOrder,
     LocalDateTime createdAt
) {
    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static FileUploadResponseDto from(Files file) {
        return new FileUploadResponseDto(
                file.getId(),
                file.getPost().getId(), // Post 엔티티에 접근
                file.getFileName(),
                file.getFileType(),
                file.getFileSize(),
                file.getFileUrl(),
                file.getSortOrder(),
                file.getCreatedAt()
        );
    }
}
