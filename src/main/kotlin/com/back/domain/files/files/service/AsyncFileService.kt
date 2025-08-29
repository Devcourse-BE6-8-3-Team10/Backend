package com.back.domain.files.files.service

import com.back.domain.files.files.entity.Files
import com.back.domain.files.files.repository.FilesRepository
import com.back.domain.post.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AsyncFileService(
    private val filesRepository: FilesRepository,
    private val fileStorageService: FileStorageService,
    private val postRepository: PostRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(AsyncFileService::class.java)
    }


    data class FileData(
        val originalFilename: String,
        val contentType: String,
        val content: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FileData

            if (originalFilename != other.originalFilename) return false
            if (contentType != other.contentType) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = originalFilename.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }

    // 파일 업로드 서비스 (비동기 호출)
    @Async
    @Transactional
    fun uploadFilesAsync(postId: Long, fileDataList: List<FileData>) {
        if (fileDataList.isEmpty()) {
            log.info("업로드할 파일 데이터가 없어 비동기 작업을 종료합니다.")
            return
        }

        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("비동기 처리 중 게시글을 찾을 수 없습니다: $postId") }

        val lastFileResult = filesRepository.findLastByPostIdWithLock(postId, PageRequest.of(0, 1))
        var sortOrder = if (lastFileResult.isEmpty()) 1 else lastFileResult[0].getSortOrder() + 1

        log.info("비동기 파일 처리 시작. 게시글 ID: {}, 파일 개수: {}", postId, fileDataList.size)

        for (fileData in fileDataList) {
            val fileUrl: String? = try {
                // Pass byte array to storage service
                fileStorageService.storeFile(
                    fileData.content,
                    fileData.originalFilename,
                    fileData.contentType,
                    "post_${post.getId()}"
                )
            } catch (e: RuntimeException) {
                log.error("물리 파일 저장 실패, 건너뜁니다: ${fileData.originalFilename}", e)
                continue
            }

            // Save metadata to database
            filesRepository.save(
                Files(
                    post,
                    fileData.originalFilename,
                    fileData.contentType,
                    fileData.content.size.toLong(),
                    fileUrl!!,
                    sortOrder++
                )
            )
        }
        log.info("게시글 ID {}에 대한 파일 업로드가 비동기적으로 완료되었습니다.", postId)
    }
}