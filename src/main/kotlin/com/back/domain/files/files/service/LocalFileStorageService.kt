package com.back.domain.files.files.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Service
@Profile("dev")
class LocalFileStorageService : FileStorageService {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    @Value("\${file.upload.max-size:10485760}")
    private var maxFileSize: Long = 0

    companion object {
        private val log = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    }

    override fun storeFile(fileContent: ByteArray, originalFilename: String, contentType: String, subFolder: String): String {
        if (fileContent.size > maxFileSize) {
            throw RuntimeException("파일 크기가 너무 큽니다. 최대 ${maxFileSize / (1024 * 1024)}MB까지 업로드 가능합니다.")
        }

        if (!isAllowedFileType(contentType)) {
            throw RuntimeException("허용되지 않는 파일 형식입니다.")
        }

        try {
            val uploadPath = Paths.get(uploadDir, subFolder).toAbsolutePath().normalize()
            Files.createDirectories(uploadPath)

            val fileExtension = getExtension(originalFilename)
            val uniqueFileName = UUID.randomUUID().toString() + fileExtension
            val targetLocation = uploadPath.resolve(uniqueFileName)

            Files.write(targetLocation, fileContent)

            return "/files/$subFolder/$uniqueFileName"
        } catch (e: IOException) {
            throw RuntimeException("로컬 파일 시스템에 파일 저장 실패: ${e.message}", e)
        }
    }

    override fun deletePhysicalFile(fileUrl: String) {
        if (fileUrl.isBlank()) {
            return
        }

        try {
            val relativePath = fileUrl.substring("/files/".length)
            val filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize()
            if (Files.exists(filePath)) {
                Files.delete(filePath)
            } else {
                throw RuntimeException("로컬 파일 시스템에서 파일을 찾을 수 없어 삭제 실패: $fileUrl")
            }
        } catch (e: IOException) {
            throw RuntimeException("로컬 파일 시스템에서 파일 삭제 실패: ${e.message}", e)
        }
    }

    override fun loadFileAsResource(fileUrl: String): Resource {
        try {
            val relativePath = fileUrl.substring("/files/".length)
            val filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize()
            val resource = UrlResource(filePath.toUri())
            if (resource.exists() && resource.isReadable) {
                return resource
            } else {
                throw RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: $fileUrl")
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException("파일 URL 형식이 잘못되었습니다: $fileUrl", e)
        }
    }

    private fun getExtension(fileName: String?): String {
        if (fileName.isNullOrEmpty()) {
            return ""
        }
        val dotIndex = fileName.lastIndexOf(".")
        return if (dotIndex != -1) fileName.substring(dotIndex) else ""
    }

    private fun isAllowedFileType(contentType: String): Boolean {
        return contentType.startsWith("image/") ||
                contentType == "application/pdf" ||
                contentType.startsWith("text/")
    }
}