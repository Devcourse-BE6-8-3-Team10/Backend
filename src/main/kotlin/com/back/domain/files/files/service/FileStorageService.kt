package com.back.domain.files.files.service

import org.springframework.core.io.Resource

interface FileStorageService {
    fun storeFile(fileContent: ByteArray, originalFilename: String, contentType: String, subFolder: String): String
    fun deletePhysicalFile(fileUrl: String)
    fun loadFileAsResource(fileUrl: String): Resource
}
