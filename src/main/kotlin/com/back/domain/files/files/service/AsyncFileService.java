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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileService {

    private final FilesRepository filesRepository;
    private final FileStorageService fileStorageService;
    private final PostRepository postRepository;

    @Async // 비동기적으로 실행되도록 설정
    @Transactional
    public void uploadFilesAsync(Long postId, MultipartFile[] files) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("비동기 처리 중 게시글을 찾을 수 없습니다: " + postId));

        // sortOrder 시작 값 설정 (Race Condition Fix)
        List<Files> lastFileResult = filesRepository.findLastByPostIdWithLock(postId, PageRequest.of(0, 1));
        int sortOrder = lastFileResult.isEmpty() ? 1 : lastFileResult.get(0).getSortOrder() + 1;


        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.trim().isEmpty()) {
                    continue;
                }

                // fileType null 체크 및 기본값 설정
                String fileType = file.getContentType();
                if (fileType == null || fileType.isBlank()) {
                    fileType = "application/octet-stream";
                }

                long fileSize = file.getSize();

                String fileUrl = null;
                try {
                    // 파일을 스토리지에 저장하고 URL을 받아옴
                    fileUrl = fileStorageService.storeFile(file, "post_" + post.getId());
                } catch (RuntimeException e) {
                    log.error("파일 저장 실패, 건너뜀: " + fileName, e);
                    continue;
                }

                // 파일 메타데이터를 데이터베이스에 저장
                filesRepository.save(
                        Files.builder()
                                .post(post)
                                .fileName(fileName)
                                .fileType(fileType)
                                .fileSize(fileSize)
                                .fileUrl(fileUrl)
                                .sortOrder(sortOrder++)
                                .build()
                );
            }
        }
        log.info("게시글 ID {}에 대한 파일 업로드가 비동기적으로 완료되었습니다.", post.getId());
    }
}
