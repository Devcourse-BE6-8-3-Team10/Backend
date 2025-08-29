package com.back.domain.files.files.dto

import com.back.domain.files.files.entity.Files
import java.time.LocalDateTime

data class FileUploadResponseDto(
    // 파일 업로드 응답 DTO
    val id: Long,
    val postId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val fileUrl: String,
    val sortOrder: Int,
    val createdAt: LocalDateTime?
) {
    companion object {
        // Entity를 DTO로 변환하는 정적 팩토리 메서드
        @JvmStatic
        fun from(file: Files): FileUploadResponseDto {
            return FileUploadResponseDto(
                id = file.id,
                postId = file.post.id, // Post 엔티티에 접근
                fileName = file.fileName,
                fileType = file.fileType,
                fileSize = file.fileSize,
                fileUrl = file.fileUrl,
                sortOrder = file.sortOrder,
                createdAt = file.createdAt
            )
        }
    }
}