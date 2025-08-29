package com.back.domain.files.files.service;

import com.back.domain.files.files.entity.Files;
import com.back.domain.files.files.repository.FilesRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileService {

    // Helper DTO to safely pass file data across threads
    public record FileData(String originalFilename, String contentType, byte[] content) {}

    private final FilesRepository filesRepository;
    private final FileStorageService fileStorageService;
    private final PostRepository postRepository;

    @Async
    @Transactional
    public void uploadFilesAsync(Long postId, List<FileData> fileDataList) {
        if (fileDataList == null || fileDataList.isEmpty()) {
            log.info("업로드할 파일 데이터가 없어 비동기 작업을 종료합니다.");
            return;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("비동기 처리 중 게시글을 찾을 수 없습니다: " + postId));

        List<Files> lastFileResult = filesRepository.findLastByPostIdWithLock(postId, PageRequest.of(0, 1));
        int sortOrder = lastFileResult.isEmpty() ? 1 : lastFileResult.get(0).getSortOrder() + 1;

        log.info("비동기 파일 처리 시작. 게시글 ID: {}, 파일 개수: {}", postId, fileDataList.size());

        for (FileData fileData : fileDataList) {
            String fileUrl = null;
            try {
                // Pass byte array to storage service
                fileUrl = fileStorageService.storeFile(fileData.content(), fileData.originalFilename(), fileData.contentType(), "post_" + post.getId());
            } catch (RuntimeException e) {
                log.error("물리 파일 저장 실패, 건너뜁니다: " + fileData.originalFilename(), e);
                continue;
            }

            // Save metadata to database
            filesRepository.save(
                    Files.builder()
                            .post(post)
                            .fileName(fileData.originalFilename())
                            .fileType(fileData.contentType())
                            .fileSize(fileData.content().length)
                            .fileUrl(fileUrl)
                            .sortOrder(sortOrder++)
                            .build()
            );
        }
        log.info("게시글 ID {}에 대한 파일 업로드가 비동기적으로 완료되었습니다.", postId);
    }
}
