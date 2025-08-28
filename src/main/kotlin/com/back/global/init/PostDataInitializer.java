package com.back.global.init;

import com.back.domain.files.files.entity.Files;
import com.back.domain.files.files.repository.FilesRepository;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Profile({"dev", "prod"})
@Component
@RequiredArgsConstructor
@Slf4j
public class PostDataInitializer {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final FilesRepository filesRepository;

    // MultipartFile을 대체하는 내부 클래스
    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public CustomMultipartFile(byte[] bytes, String name, String originalFilename, String contentType) {
            this.bytes = bytes;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }

    @PostConstruct
    @Transactional
    public void init() {
        log.info("===== 게시글 테스트 데이터 생성 시작 =====");

        Member user1 = memberRepository.findByEmail("test1@user.com")
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email("test1@user.com").password(passwordEncoder.encode("1234"))
                        .name("김혁신").role(Role.USER).build()));

        Member user2 = memberRepository.findByEmail("test2@user.com")
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email("test2@user.com").password(passwordEncoder.encode("1234"))
                        .name("박기술").role(Role.USER).build()));

        if (postRepository.count() > 0) {
            log.info("게시글 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        List<MultipartFile> sampleImages = loadSampleImages();

        // 샘플 데이터 리스트 생성
        List<PostData> postDataList = List.of(
            new PostData(user1, "AI 기반 음성인식 알고리즘 특허", "혁신적인 음성인식 기술로, 다양한 언어를 실시간으로 정확하게 인식하고 텍스트로 변환합니다.", Post.Category.PRODUCT, 15000000),
            new PostData(user2, "차세대 고효율 배터리 기술", "기존 리튬이온 배터리보다 2배 이상 높은 에너지 밀도를 자랑하는 차세대 배터리 기술입니다.", Post.Category.METHOD, 25000000),
            new PostData(user1, "원격 의료 진단 시스템 특허", "AI를 활용하여 환자의 상태를 원격으로 진단하고, 맞춤형 의료 서비스를 제공하는 혁신적인 시스템입니다.", Post.Category.USE, 18500000),
            new PostData(user2, "친환경 생분해성 플라스틱 대체 기술", "옥수수 전분을 원료로 하여 6개월 내에 자연 분해되는 친환경 플라스틱 대체 기술에 대한 특허입니다.", Post.Category.PRODUCT, 12000000),
            new PostData(user1, "자율주행 차량용 센서 융합 기술", "라이다, 레이더, 카메라 등 다양한 센서 데이터를 융합하여 악천후 속에서도 안정적인 자율주행을 지원합니다.", Post.Category.METHOD, 30000000),
            new PostData(user2, "스마트폰 기반 생체 보안 인증", "사용자의 홍채와 지문을 동시에 인식하여, 현존 최고 수준의 보안을 제공하는 스마트폰 인증 기술입니다.", Post.Category.TRADEMARK, 8900000),
            new PostData(user1, "고효율 태양광 패널 제조 공법", "특수 나노 코팅 기술을 적용하여, 기존 대비 30% 이상 발전 효율을 높인 태양광 패널 제조 공법입니다.", Post.Category.METHOD, 22000000)
        );

        for (PostData data : postDataList) {
            Post post = createAndSavePost(data.member, data.title, data.description, data.category, data.price);
            addRandomImagesToPost(post, sampleImages);
        }

        log.info("===== 게시글 테스트 데이터 생성 완료 =====");
    }

    private Post createAndSavePost(Member member, String title, String description, Post.Category category, int price) {
        Post post = Post.builder()
                .member(member)
                .title(title)
                .description(description)
                .category(category)
                .price(price)
                .status(Post.Status.SALE)
                .build();
        return postRepository.save(post);
    }

    private List<MultipartFile> loadSampleImages() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<MultipartFile> sampleImages = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources("classpath:sample-images/*.{jpg,png,gif}");
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] bytes = inputStream.readAllBytes();
                    String contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
                    sampleImages.add(new CustomMultipartFile(
                            bytes,
                            resource.getFilename(),
                            resource.getFilename(),
                            contentType != null ? contentType : "application/octet-stream"
                    ));
                }
            }
        } catch (IOException e) {
            log.error("샘플 이미지 로드 실패", e);
        }
        return sampleImages;
    }

    private void addRandomImagesToPost(Post post, List<MultipartFile> images) {
        if (images.isEmpty()) return;

        Random random = new Random();
        int imageCount = 1 + random.nextInt(Math.min(3, images.size()));
        Collections.shuffle(images);

        for (int i = 0; i < imageCount; i++) {
            MultipartFile file = images.get(i);
            try {
                String fileUrl = fileStorageService.storeFile(file, "post_" + post.getId());
                Files fileEntity = Files.builder()
                        .post(post)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .fileUrl(fileUrl)
                        .sortOrder(i + 1)
                        .build();
                filesRepository.save(fileEntity);
            } catch (Exception e) {
                log.error("게시물 {}에 이미지 첨부 실패: {}", post.getId(), file.getOriginalFilename(), e);
            }
        }
    }

    // 샘플 데이터 저장을 위한 내부 클래스
    private static class PostData {
        Member member;
        String title;
        String description;
        Post.Category category;
        int price;

        PostData(Member member, String title, String description, Post.Category category, int price) {
            this.member = member;
            this.title = title;
            this.description = description;
            this.category = category;
            this.price = price;
        }
    }
}
