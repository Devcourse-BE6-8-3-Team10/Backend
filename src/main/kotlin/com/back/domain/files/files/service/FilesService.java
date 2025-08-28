// FilesService.java
package com.back.domain.files.files.service;

import com.back.domain.files.files.dto.FileUploadResponseDto;
import com.back.domain.files.files.entity.Files;
import com.back.domain.files.files.repository.FilesRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async; // 새롭게 추가된 import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FilesService {

    private final FilesRepository filesRepository;
    private final FileStorageService fileStorageService;
    private final PostRepository postRepository;
    private final Rq rq;

    // 파일 업로드 서비스 (동기 호출)
    public RsData<String> uploadFiles(Long postId, MultipartFile[] files) { // 반환 타입을 RsData<String>으로 변경
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다: " + postId));

        if (rq.getMember() == null || !rq.getMember().getId().equals(post.getMember().getId())) {
            throw new IllegalArgumentException("게시글 작성자만 파일을 업로드할 수 있습니다.");
        }

        // 비동기 파일 업로드 메서드 호출
        uploadFilesAsync(post, files);

        // 파일 처리 시작을 알리는 즉각적인 응답
        return new RsData<>(
                "200",
                "파일 업로드가 시작되었습니다. 완료 시 별도의 알림은 전송되지 않습니다.",
                "Upload initiated"
        );
    }

    @Async // 비동기적으로 실행되도록 설정
    @Transactional
    public void uploadFilesAsync(Post post, MultipartFile[] files) {
        List<FileUploadResponseDto> responseList = new ArrayList<>();
        int sortOrder = 1;

        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.trim().isEmpty()) {
                    continue;
                }

                String fileType = file.getContentType();
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


    // 게시글 ID로 파일 조회 서비스
    public RsData<List<FileUploadResponseDto>> getFilesByPostId(Long postId) {
        List<Files> files = filesRepository.findByPostIdOrderBySortOrderAsc(postId);

        List<FileUploadResponseDto> result = files.stream()
                .map(FileUploadResponseDto::from)
                .toList();

        return new RsData<>(
                "200",
                result.isEmpty() ? "첨부된 파일이 없습니다." : "파일 목록 조회 성공",
                result
        );
    }

    // 파일 개별 삭제 서비스
    public RsData<Void> deleteFile(Long postId, Long fileId) {

        Files file = filesRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다: " + fileId));

        if (!file.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글에 속하지 않는 파일입니다: " + fileId);
        }

        if (!rq.isLogin()) {
            throw new IllegalArgumentException("로그인 후 이용해 주세요.");
        }

        Long currentMemberId = rq.getMemberId();
        if (!file.getPost().getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("해당 파일을 삭제할 권한이 없습니다. 현재 사용자 ID: " + currentMemberId);
        }

        deletePhysicalFileSafely(file.getFileUrl());

        filesRepository.deleteById(fileId);
        return new RsData("200", "파일 삭제 성공", null);
    }

    // =================== 관리자 전용 서비스 구역 ===================

    public RsData<Page<FileUploadResponseDto>> adminGetAllFiles(Pageable pageable) {
        if (!rq.isAdmin()) {
            return new RsData<>("403-1", "관리자 권한이 필요합니다.", null);
        }

        Page<Files> filesPage = filesRepository.findAll(pageable);

        Page<FileUploadResponseDto> dtoPage = filesPage.map(FileUploadResponseDto::from);

        return new RsData<>("200", dtoPage.isEmpty() ? "등록된 파일이 없습니다." : "파일 목록 조회 성공", dtoPage);
    }

    public RsData<FileUploadResponseDto> adminGetFileById(Long fileId) {
        if (!rq.isAdmin()) {
            return new RsData<>("403-2", "관리자 권한이 필요합니다.", null);
        }
        Files file = filesRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다: " + fileId));

        return new RsData<>("200", "파일 조회 성공 (관리자)", FileUploadResponseDto.from(file));
    }

    public RsData<Void> adminDeleteFile(Long fileId) {
        if (!rq.isAdmin()) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        Files file = filesRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다. " + fileId));

        deletePhysicalFileSafely(file.getFileUrl());

        filesRepository.deleteById(fileId);
        return new RsData<>("200", "파일 삭제 성공 (관리자)", null);
    }


    // ==============헬퍼 메서드 영역 ==============
    private void deletePhysicalFileSafely(String fileUrl) {
        try {
            fileStorageService.deletePhysicalFile(fileUrl);
        } catch (Exception e) {
            log.error("물리 파일 삭제 중 오류 발생: " + fileUrl, e);
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }
}