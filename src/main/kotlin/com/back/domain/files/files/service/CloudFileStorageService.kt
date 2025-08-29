package com.back.domain.files.files.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.net.MalformedURLException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

@Service
@Profile("prod") // 프로덕션 환경에서만 이 서비스가 활성화되도록 설정
class CloudFileStorageService(
    // Google Cloud Storage 클라이언트 객체
    // 생성자를 통한 Storage 객체 주입 (Spring이 자동으로 StorageOptions.getDefaultInstance().getService()를 통해 생성)
    private val gcsStorage: Storage
) : FileStorageService {

    companion object {
        private val log = LoggerFactory.getLogger(CloudFileStorageService::class.java)
    }

    // 클라우드 스토리지 버킷 이름 설정 (application.yml에서 주입)
    @Value("\${GCP_BUCKET_NAME}")
    private lateinit var bucketName: String

    @Value("\${file.upload.max-size:10485760}")
    private var maxFileSize: Long = 0 // 최대 파일 크기 (기본값: 10MB)

    override fun storeFile(fileContent: ByteArray, originalFilename: String, contentType: String, subFolder: String): String {
        if (fileContent.size > maxFileSize) {
            throw RuntimeException("파일 크기가 너무 큽니다. 최대 ${maxFileSize / (1024 * 1024)}MB까지 업로드 가능합니다.")
        }

        if (!isAllowedFileType(contentType)) {
            throw RuntimeException("허용되지 않는 파일 형식입니다.")
        }

        val fileExtension = getExtension(originalFilename)
        // subFolder (예: "profile/{memberId}")와 UUID를 조합하여 고유한 객체 이름 생성
        val fileNameInStorage = "$subFolder/${UUID.randomUUID()}$fileExtension"

        val blobId = BlobId.of(bucketName, fileNameInStorage)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .build()

        return try {
            gcsStorage.create(blobInfo, fileContent)
            "https://storage.googleapis.com/$bucketName/$fileNameInStorage"
        } catch (e: Exception) {
            throw RuntimeException("클라우드 스토리지 파일 저장 중 오류가 발생했습니다.", e)
        }
    }

    override fun deletePhysicalFile(fileUrl: String) {
        if (fileUrl.isBlank()) {
            return
        }

        val gcsUrlPrefix = "https://storage.googleapis.com/$bucketName/"
        if (!fileUrl.startsWith(gcsUrlPrefix)) {
            return
        }

        val objectNameToDelete = URLDecoder.decode(
            fileUrl.substring(gcsUrlPrefix.length),
            StandardCharsets.UTF_8
        )

        try {
            val deleted = gcsStorage.delete(BlobId.of(bucketName, objectNameToDelete))
            if (!deleted) {
                throw RuntimeException("클라우드 스토리지에서 파일 삭제 실패 (파일을 찾을 수 없거나 권한 없음): $fileUrl")
            }
        } catch (e: Exception) {
            throw RuntimeException("클라우드 스토리지 파일 삭제 중 오류가 발생했습니다.", e)
        }
    }

    override fun loadFileAsResource(fileUrl: String): Resource {
        return try {
            UrlResource(fileUrl)
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