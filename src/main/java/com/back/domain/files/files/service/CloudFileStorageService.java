package com.back.domain.files.files.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod") // 프로덕션 환경에서만 이 서비스가 활성화되도록 설정
public class CloudFileStorageService implements FileStorageService {
    // 클라우드 스토리지 버킷 이름 설정 (application.yml에서 주입)
    @Value("${GCP_BUCKET_NAME}")
    private String bucketName;

    // Google Cloud Storage 클라이언트 객체
    private final Storage gcsStorage;

    // 생성자를 통한 Storage 객체 주입 (Spring이 자동으로 StorageOptions.getDefaultInstance().getService()를 통해 생성)
    public CloudFileStorageService(Storage gcsStorage) {
        this.gcsStorage = gcsStorage;
    }

    @Value(("${file.upload.max-size:10485760}"))
    private long maxFileSize; // 최대 파일 크기 (기본값: 10MB)

    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("파일 크기가 너무 큽니다. 최대 " + (maxFileSize / (1024 * 1024)) + "MB까지 업로드 가능합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedFileType(contentType)) {
            throw new RuntimeException("허용되지 않는 파일 형식입니다.");
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = getExtension(originalFileName);
        // subFolder (예: "profile/{memberId}")와 UUID를 조합하여 고유한 객체 이름 생성
        String fileNameInStorage = subFolder + "/" + UUID.randomUUID().toString() + fileExtension;

        BlobId blobId = BlobId.of(bucketName, fileNameInStorage);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        try {
            gcsStorage.createFrom(blobInfo, file.getInputStream());
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileNameInStorage);
        } catch (IOException e) {
            throw new RuntimeException("클라우드 스토리지 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        String gcsUrlPrefix = String.format("https://storage.googleapis.com/%s/", bucketName);
        if (!fileUrl.startsWith(gcsUrlPrefix)) {
            return;
        }

        String objectNameToDelete = URLDecoder.decode(
                fileUrl.substring(gcsUrlPrefix.length()),
                StandardCharsets.UTF_8
        );

        try {
            boolean deleted = gcsStorage.delete(BlobId.of(bucketName, objectNameToDelete));
            if (!deleted) {
                throw new RuntimeException("클라우드 스토리지에서 파일 삭제 실패 (파일을 찾을 수 없거나 권한 없음): " + fileUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("클라우드 스토리지 파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileUrl) {
        try {
            return new UrlResource(fileUrl);
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
