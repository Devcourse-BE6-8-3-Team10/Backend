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
    private val gcsStorage: Storage
) : FileStorageService {

    companion object {
        private val log = LoggerFactory.getLogger(CloudFileStorageService::class.java)
    }

    @Value("\${GCP_BUCKET_NAME}")
    private lateinit var bucketName: String

    @Value("\${file.upload.max-size:10485760}")
    private var maxFileSize: Long = 0

    override fun storeFile(fileContent: ByteArray, originalFilename: String, contentType: String, subFolder: String): String {
        if (fileContent.size > maxFileSize) {
            throw RuntimeException("파일 크기가 너무 큽니다. 최대 ${maxFileSize / (1024 * 1024)}MB까지 업로드 가능합니다.")
        }

        if (!isAllowedFileType(contentType)) {
            throw RuntimeException("허용되지 않는 파일 형식입니다.")
        }

        // subFolder 정규화 - 상대경로 제거
        val normalizedSubFolder = sanitizeSubFolder(subFolder)

        val fileExtension = getExtension(originalFilename)
        val fileNameInStorage = "$normalizedSubFolder/${UUID.randomUUID()}$fileExtension"

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

        val objectNameToDelete = validateAndExtractObjectName(fileUrl) ?: return

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
        //  적절한 검증 후 공개 접근 허용
        validateGcsUrl(fileUrl)

        return try {
            UrlResource(fileUrl)
        } catch (e: MalformedURLException) {
            throw RuntimeException("파일 URL 형식이 잘못되었습니다: $fileUrl", e)
        }
    }

    // ============== 헬퍼 메서드 영역 ==============

    /**
     * GCS URL 검증 - 우리 버킷의 URL인지만 확인
     */
    private fun validateGcsUrl(fileUrl: String) {
        val gcsUrlPrefix = "https://storage.googleapis.com/$bucketName/"
        if (!fileUrl.startsWith(gcsUrlPrefix)) {
            throw RuntimeException("허용되지 않는 파일 URL입니다: $fileUrl")
        }
    }

    /**
     * 파일 URL에서 object name 추출 및 검증
     */
    private fun validateAndExtractObjectName(fileUrl: String): String? {
        val gcsUrlPrefix = "https://storage.googleapis.com/$bucketName/"
        if (!fileUrl.startsWith(gcsUrlPrefix)) {
            log.warn("GCS URL이 아님: $fileUrl")
            return null
        }

        return URLDecoder.decode(
            fileUrl.substring(gcsUrlPrefix.length),
            StandardCharsets.UTF_8
        )
    }

    /**
     * 서브폴더명 정규화 (위험한 문자 제거)
     */
    private fun sanitizeSubFolder(subFolder: String): String {
        return subFolder.replace("..", "").replace("./", "").trim('/', ' ')
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