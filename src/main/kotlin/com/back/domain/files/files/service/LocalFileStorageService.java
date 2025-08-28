package com.back.domain.files.files.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@Profile("dev")
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value(("${file.upload.max-size:10485760}"))
    private long maxFileSize;

    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("파일 크기가 너무 큽니다. 최대 " + (maxFileSize / (1024 * 1024)) + "MB까지 업로드 가능합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedFileType(contentType)) {
            throw new RuntimeException("허용되지 않는 파일 형식입니다.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, subFolder).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getExtension(originalFilename);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension; // 고유한 파일명 생성
            Path targetLocation = uploadPath.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/files/" + subFolder + "/" + uniqueFileName; // 로컬 접근 URL 반환
        } catch (IOException e) {
            throw new RuntimeException("로컬 파일 시스템에 파일 저장 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String relativePath = fileUrl.substring("/files/".length());
            Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                throw new RuntimeException("로컬 파일 시스템에서 파일을 찾을 수 없어 삭제 실패: " + fileUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("로컬 파일 시스템에서 파일 삭제 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileUrl) {
        try {
            String relativePath = fileUrl.substring("/files/".length());
            Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + fileUrl);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 URL 형식이 잘못되었습니다: " + fileUrl, e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex != -1 ? fileName.substring(dotIndex) : "";
    }

    private boolean isAllowedFileType(String contentType) {
        return contentType.startsWith("image/") ||
                contentType.equals("application/pdf") ||
                contentType.startsWith("text/");
    }
}
