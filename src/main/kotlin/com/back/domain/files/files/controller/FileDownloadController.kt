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

    // 안전한 inline 제공이 가능한 MIME 타입들
    private val safeInlineTypes = setOf(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "application/pdf"
    )

    // 파일 다운로드 API
    @GetMapping("/**")
    fun downloadFile(request: HttpServletRequest): ResponseEntity<Resource> {
        // contextPath 제거 후 디코딩
        val rawPath = request.requestURI.removePrefix(request.contextPath ?: "")
        val decodedPath = java.net.URLDecoder.decode(rawPath, java.nio.charset.StandardCharsets.UTF_8)
        if (!decodedPath.startsWith("/files/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file URL prefix")
        }
        // "/files/" 이후 경로만 정규화
        val rest = decodedPath.removePrefix("/files/")
        val normalized = java.nio.file.Paths.get(rest).normalize()
        val normStr = normalized.toString()
        // 절대경로/루트 이탈/백슬래시/의도치 않은 현재 디렉토리 이동 차단
        if (normalized.isAbsolute || normStr.startsWith("..") || normStr.contains("\\") || normStr.contains("/./") || normStr.contains("//")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file URL")
        }
        val sanitizedFileUrl = "/files/" + normStr.replace('\\', '/')

        // 파일 리소스 로드 (정규화된 경로로 Service 호출)
        val resource = try {
            fileStorageService.loadFileAsResource(sanitizedFileUrl)
        } catch (e: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e)
        }

        // MediaTypeFactory로 MIME 타입 결정
        val mediaType = MediaTypeFactory.getMediaType(resource.filename ?: "")
            .orElse(MediaType.APPLICATION_OCTET_STREAM)

        // 안전한 콘텐츠는 inline, 위험한 콘텐츠는 attachment로 처리
        val isInlineSafe = safeInlineTypes.contains(mediaType.toString())
        val contentDisposition = if (isInlineSafe) {
            ContentDisposition.inline()
        } else {
            ContentDisposition.attachment()
        }.filename(resource.filename ?: "download", StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .header("X-Content-Type-Options", "nosniff")  // 브라우저 스니핑 방지
            .body(resource)
    }
}