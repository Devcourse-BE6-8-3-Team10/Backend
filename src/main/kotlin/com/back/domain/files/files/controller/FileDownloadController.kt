package com.back.domain.files.files.controller

import com.back.domain.files.files.service.FileStorageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/files")
class FileDownloadController(
    private val fileStorageService: FileStorageService
) {

    // 파일 다운로드 API
    @GetMapping("/**")
    fun downloadFile(request: HttpServletRequest): ResponseEntity<Resource> {
        // contextPath를 제거하여 실제 파일 경로만 추출
        val contextPath = request.contextPath ?: ""
        val requestURI = request.requestURI
        val fileUrl = if (contextPath.isNotEmpty() && requestURI.startsWith(contextPath)) {
            requestURI.substring(contextPath.length)
        } else {
            requestURI
        }

        // 1차 방어: 기본적인 경로 순회 패턴 차단 (기존 로직 유지)
        if (fileUrl.contains("..") || fileUrl.contains("./") || fileUrl.contains("\\")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file URL")
        }

        // 파일 리소스 로드 (404 변환 처리)
        val resource = try {
            fileStorageService.loadFileAsResource(fileUrl)
        } catch (e: RuntimeException) {
            // Storage에서 발생한 예외를 404로 변환
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e)
        }

        // 안전한 MIME 타입 결정 (NPE 방지)
        val contentType = determineContentType(resource.filename)

        // 한글/비ASCII 파일명 안전 처리
        val contentDisposition = ContentDisposition.inline()
            .filename(resource.filename ?: "download", StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(resource)
    }

    /**
     * 파일명 기반 MIME 타입 결정 (NPE 방지, 파일시스템 접근 없음)
     */
    private fun determineContentType(filename: String?): String {
        if (filename == null) return "application/octet-stream"

        return when {
            filename.endsWith(".jpg", true) || filename.endsWith(".jpeg", true) -> "image/jpeg"
            filename.endsWith(".png", true) -> "image/png"
            filename.endsWith(".gif", true) -> "image/gif"
            filename.endsWith(".webp", true) -> "image/webp"
            filename.endsWith(".pdf", true) -> "application/pdf"
            filename.endsWith(".txt", true) -> "text/plain"
            filename.endsWith(".csv", true) -> "text/csv"
            filename.endsWith(".doc", true) -> "application/msword"
            filename.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            filename.endsWith(".xls", true) -> "application/vnd.ms-excel"
            filename.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            filename.endsWith(".zip", true) -> "application/zip"
            filename.endsWith(".mp4", true) -> "video/mp4"
            filename.endsWith(".mp3", true) -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }
}