package com.back.domain.files.files.controller

import com.back.domain.files.files.service.FileStorageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException

@RestController
@RequestMapping("/files")
class FileDownloadController(
    private val fileStorageService: FileStorageService
) {

    // 파일 다운로드 API
    @GetMapping("/**")
    fun downloadFile(request: HttpServletRequest): ResponseEntity<Resource> {
        val fileUrl = request.requestURI
        // 경로 순회 공격 방지
        if (fileUrl.contains("..") || fileUrl.contains("./") || fileUrl.contains("\\")) {
            throw IllegalArgumentException("Invalid file URL: $fileUrl")
        }

        val resource = fileStorageService.loadFileAsResource(fileUrl)

        var contentType: String? = null
        try {
            contentType = request.servletContext.getMimeType(resource.file.absolutePath)
        } catch (ex: IOException) {
            // fallback to the default content type if type could not be determined
            contentType = "application/octet-stream"
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
            .body(resource)
    }
}