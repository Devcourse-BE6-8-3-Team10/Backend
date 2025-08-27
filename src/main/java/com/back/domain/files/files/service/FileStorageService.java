package com.back.domain.files.files.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subFolder);
    void deletePhysicalFile(String fileUrl);
    Resource loadFileAsResource(String fileUrl);
}