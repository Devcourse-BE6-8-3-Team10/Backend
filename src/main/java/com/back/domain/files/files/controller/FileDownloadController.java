package com.back.domain.files.files.controller;

import com.back.domain.files.files.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileDownloadController {

    private final FileStorageService fileStorageService;

    // 파일 다운로드 API
    @GetMapping("/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        String fileUrl = request.getRequestURI();
        // 경로 순회 공격 방지
        if (fileUrl.contains("..") || fileUrl.contains("./") || fileUrl.contains("\\")) {
            throw new IllegalArgumentException("Invalid file URL: " + fileUrl);
        }

        Resource resource = fileStorageService.loadFileAsResource(fileUrl);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // fallback to the default content type if type could not be determined
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
