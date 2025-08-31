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
import java.nio.file.Paths

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
        val normalized = Paths.get(rest).normalize()
        val normStr = normalized.toString()

        // 크로스 플랫폼 호환 보안 검증
        // Windows에서 백슬래시는 정상이므로 Unix 경로로 변환 후 검증
        val unixPath = normStr.replace('\\', '/')

        // 절대경로/루트 이탈/의도치 않은 디렉토리 이동 차단
        if (normalized.isAbsolute ||
            unixPath.startsWith("..") ||
            unixPath.contains("/./") ||
            unixPath.contains("//") ||
            unixPath.contains("../")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file URL")
        }

        // 항상 Unix 스타일 경로로 Service에 전달 (크로스 플랫폼 호환)
        val sanitizedFileUrl = "/files/" + unixPath

        // 파일 리소스 로드
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
            .header("X-Content-Type-Options", "nosniff")
            .body(resource)
    }
}