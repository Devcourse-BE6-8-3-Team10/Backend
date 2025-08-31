package com.back.domain.files.files.service

import com.back.domain.files.files.dto.FileUploadResponseDto
import com.back.domain.files.files.repository.FilesRepository
import com.back.domain.post.repository.PostRepository
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
@Transactional
class FilesService(
    private val filesRepository: FilesRepository,
    private val fileStorageService: FileStorageService,
    private val postRepository: PostRepository,
    private val rq: Rq,
    private val asyncFileService: AsyncFileService
) {
    companion object {
        private val log = LoggerFactory.getLogger(FilesService::class.java)
    }

    // 파일 업로드 서비스 (동기 호출)
    fun uploadFiles(postId: Long, files: Array<MultipartFile>?): RsData<String> {
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 게시글입니다: $postId") }

        if (!rq.isLogin() || rq.getMemberId() != post.getMember().getId()) {
            throw IllegalArgumentException("게시글 작성자만 파일을 업로드할 수 있습니다.")
        }

        if (files == null || files.isEmpty()) {
            return RsData(
                "200",
                "업로드할 파일이 없습니다.",
                "No files to upload"
            )
        }

        val fileDataList = mutableListOf<AsyncFileService.FileData>()

        for (file in files) {
            if (file.isEmpty) {
                continue
            }

            val originalFilename = file.originalFilename
            if (originalFilename.isNullOrBlank()) {
                continue
            }

            var contentType = file.contentType
            if (contentType.isNullOrBlank()) {
                contentType = "application/octet-stream"
            }

            try {
                val content = file.bytes
                fileDataList.add(AsyncFileService.FileData(originalFilename, contentType, content))
            } catch (e: IOException) {
                log.error("파일을 읽는 중 오류가 발생했습니다: $originalFilename", e)
                // 실패한 파일은 건너뛰고 계속 진행
            }
        }

        // 비동기 서비스에 바이트 배열 목록 전달
        asyncFileService.uploadFilesAsync(post.getId(), fileDataList)

        // 파일 처리 시작을 알리는 즉각적인 응답
        return RsData(
            "200",
            "파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다.",
            "Upload initiated"
        )
    }

    // 게시글 ID로 파일 조회 서비스
    fun getFilesByPostId(postId: Long): RsData<List<FileUploadResponseDto>> {
        val files = filesRepository.findWithPostByPostId(postId)

        val result = files.map { FileUploadResponseDto.from(it) }

        return RsData(
            "200",
            if (result.isEmpty()) "첨부된 파일이 없습니다." else "파일 목록 조회 성공",
            result
        )
    }

    // 파일 개별 삭제 서비스
    fun deleteFile(postId: Long, fileId: Long): RsData<Void?> {
        val file = filesRepository.findById(fileId)
            .orElseThrow { IllegalArgumentException("파일이 존재하지 않습니다: $fileId") }

        if (file.post.getId() != postId) {
            throw IllegalArgumentException("해당 게시글에 속하지 않는 파일입니다: $fileId")
        }

        if (!rq.isLogin()) {
            throw IllegalArgumentException("로그인 후 이용해 주세요.")
        }

        val currentMemberId = rq.getMemberId()
        if (file.post.getMember().getId() != currentMemberId) {
            throw IllegalArgumentException("해당 파일을 삭제할 권한이 없습니다. 현재 사용자 ID: $currentMemberId")
        }

        deletePhysicalFileSafely(file.fileUrl)

        filesRepository.deleteById(fileId)
        return RsData("200", "파일 삭제 성공", null)
    }

    // =================== 관리자 전용 서비스 구역 ===================

    fun adminGetAllFiles(pageable: Pageable): RsData<Page<FileUploadResponseDto>> {
        if (!rq.isAdmin()) {
            return RsData("403-1", "관리자 권한이 필요합니다.", null)
        }

        val filesPage = filesRepository.findAll(pageable)
        val dtoPage = filesPage.map { FileUploadResponseDto.from(it) }

        return RsData("200", if (dtoPage.isEmpty) "등록된 파일이 없습니다." else "파일 목록 조회 성공", dtoPage)
    }

    fun adminGetFileById(fileId: Long): RsData<FileUploadResponseDto> {
        if (!rq.isAdmin()) {
            return RsData("403-2", "관리자 권한이 필요합니다.", null)
        }

        val file = filesRepository.findById(fileId)
            .orElseThrow { IllegalArgumentException("파일이 존재하지 않습니다: $fileId") }

        return RsData("200", "파일 조회 성공 (관리자)", FileUploadResponseDto.from(file))
    }

    fun adminDeleteFile(fileId: Long): RsData<Void?> {
        if (!rq.isAdmin()) {
            return RsData("403-3", "관리자 권한이 필요합니다.", null)
        }

        val file = filesRepository.findById(fileId)
            .orElseThrow { IllegalArgumentException("파일이 존재하지 않습니다. $fileId") }

        deletePhysicalFileSafely(file.fileUrl)

        filesRepository.deleteById(fileId)
        return RsData("200", "파일 삭제 성공 (관리자)", null)
    }

    // ==============헬퍼 메서드 영역 ==============
    private fun deletePhysicalFileSafely(fileUrl: String) {
        try {
            fileStorageService.deletePhysicalFile(fileUrl)
        } catch (e: Exception) {
            log.error("물리 파일 삭제 중 오류 발생 (논리 삭제는 계속 진행): $fileUrl", e)
        }
    }
}