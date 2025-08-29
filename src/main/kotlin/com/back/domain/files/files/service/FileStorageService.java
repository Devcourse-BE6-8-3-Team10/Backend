package com.back.domain.files.files.service;

import org.springframework.core.io.Resource;

public interface FileStorageService {
    String storeFile(byte[] fileContent, String originalFilename, String contentType, String subFolder);
    void deletePhysicalFile(String fileUrl);
    Resource loadFileAsResource(String fileUrl);
}
