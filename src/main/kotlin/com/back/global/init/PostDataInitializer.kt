package com.back.global.init

import com.back.domain.files.files.entity.Files
import com.back.domain.files.files.repository.FilesRepository
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.entity.Member
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLConnection
import kotlin.math.min

@Configuration
@Profile("dev", "prod")
class PostDataInitializer(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val passwordEncoder: PasswordEncoder,
    private val fileStorageService: FileStorageService,
    private val filesRepository: FilesRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @Order(1)
    fun postDataInitRunner(): ApplicationRunner = ApplicationRunner { init() }

    @Transactional
    fun init(): Unit {
        log.info("===== 게시글 테스트 데이터 생성 시작 =====")

        val user1: Member = memberRepository.findByEmail("test1@user.com")
            .orElseGet {
                memberRepository.save(
                    Member(
                        "test1@user.com",
                        passwordEncoder.encode("1234"),
                        "김혁신"
                    )
                )
            }

        val user2: Member = memberRepository.findByEmail("test2@user.com")
            .orElseGet {
                memberRepository.save(
                    Member(
                        "test2@user.com",
                        passwordEncoder.encode("1234"),
                        "박기술"
                    )
                )
            }

        if (postRepository.count() > 0L) {
            log.info("게시글 데이터가 이미 존재합니다. 초기화를 건너뜁니다.")
            return
        }

        val sampleImages: List<MultipartFile> = loadSampleImages("classpath:sample-images/*.{jpg,png,gif}")

        val postDataList: List<PostData> = listOf(
            PostData(
                member = user1,
                title = "AI 기반 음성인식 알고리즘 특허",
                description = "혁신적인 음성인식 기술로, 다양한 언어를 실시간으로 정확하게 인식하고 텍스트로 변환합니다.",
                category = Post.Category.PRODUCT,
                price = 15_000_000
            ),
            PostData(
                member = user2,
                title = "차세대 고효율 배터리 기술",
                description = "기존 리튬이온 배터리보다 2배 이상 높은 에너지 밀도를 자랑하는 차세대 배터리 기술입니다.",
                category = Post.Category.METHOD,
                price = 25_000_000
            ),
            PostData(
                member = user1,
                title = "원격 의료 진단 시스템 특허",
                description = "AI를 활용하여 환자의 상태를 원격으로 진단하고, 맞춤형 의료 서비스를 제공하는 혁신적인 시스템입니다.",
                category = Post.Category.USE,
                price = 18_500_000
            ),
            PostData(
                member = user2,
                title = "친환경 생분해성 플라스틱 대체 기술",
                description = "옥수수 전분을 원료로 하여 6개월 내에 자연 분해되는 친환경 플라스틱 대체 기술에 대한 특허입니다.",
                category = Post.Category.PRODUCT,
                price = 12_000_000
            ),
            PostData(
                member = user1,
                title = "자율주행 차량용 센서 융합 기술",
                description = "라이다, 레이더, 카메라 등 다양한 센서 데이터를 융합하여 악천후 속에서도 안정적인 자율주행을 지원합니다.",
                category = Post.Category.METHOD,
                price = 30_000_000
            ),
            PostData(
                member = user2,
                title = "스마트폰 기반 생체 보안 인증",
                description = "사용자의 홍채와 지문을 동시에 인식하여, 현존 최고 수준의 보안을 제공하는 스마트폰 인증 기술입니다.",
                category = Post.Category.TRADEMARK,
                price = 8_900_000
            ),
            PostData(
                member = user1,
                title = "고효율 태양광 패널 제조 공법",
                description = "특수 나노 코팅 기술을 적용하여, 기존 대비 30% 이상 발전 효율을 높인 태양광 패널 제조 공법입니다.",
                category = Post.Category.METHOD,
                price = 22_000_000
            )
        )

        postDataList.forEach { data: PostData ->
            val post: Post = createAndSavePost(
                member = data.member,
                title = data.title,
                description = data.description,
                category = data.category,
                price = data.price
            )
            addRandomImagesToPost(post, sampleImages)
        }

        log.info("===== 게시글 테스트 데이터 생성 완료 =====")
    }

    /**
     * Post.builder() 제거 → 편의 생성자 호출
     * (Post 엔티티에 생성자 필요: public Post(Member, String, String, Category, Integer, Status))
     */
    private fun createAndSavePost(
        member: Member,
        title: String,
        description: String,
        category: Post.Category,
        price: Int
    ): Post {
        val post: Post = Post(
            member,
            title,
            description,
            category,
            price,
            Post.Status.SALE
        )
        return postRepository.save(post)
    }

    /**
     * 샘플 이미지 로드 (타입 명시로 순환 추론 방지)
     */
    private fun loadSampleImages(pattern: String): List<MultipartFile> {
        val resolver = PathMatchingResourcePatternResolver()

        val resources: Array<Resource> = try {
            resolver.getResources(pattern)
        } catch (e: Exception) {
            log.error("샘플 이미지 리소스를 찾을 수 없습니다. pattern={}", pattern, e)
            emptyArray()
        }

        val files: List<MultipartFile> = resources.mapNotNull { res: Resource ->
            val mf: MultipartFile? = runCatching {
                res.inputStream.use { input: InputStream ->
                    val bytes: ByteArray = input.readAllBytes()
                    val filename: String = res.filename ?: "sample"
                    val contentType: String = URLConnection.guessContentTypeFromName(filename) ?: "application/octet-stream"
                    CustomMultipartFile(bytes, filename, filename, contentType) as MultipartFile
                }
            }.getOrNull()
            mf
        }

        return files
    }

    /**
     * 게시글에 랜덤 이미지 첨부
     */
    private fun addRandomImagesToPost(post: Post, images: List<MultipartFile>): Unit {
        if (images.isEmpty()) return

        val count: Int = 1 + kotlin.random.Random.nextInt(min(3, images.size))
        val picks: List<MultipartFile> = images.shuffled().take(count)

        picks.forEachIndexed { idx: Int, file: MultipartFile ->
            runCatching {
                val fileUrl: String = fileStorageService.storeFile(
                    file.bytes,
                    file.originalFilename,
                    file.contentType,
                    "post_${post.id}"
                )
                val entity = Files(
                    post = post,
                    fileName = file.originalFilename ?: "unknown",
                    fileType = file.contentType ?: "application/octet-stream",
                    fileSize = file.size,
                    fileUrl = fileUrl,
                    sortOrder = idx + 1
                )
                filesRepository.save(entity)
            }.onFailure {
                log.error("게시물 {}에 이미지 첨부 실패: {}", post.id, file.originalFilename, it)
            }
        }
    }

    /**
     * 간단한 MultipartFile 구현체 (내부 클래스)
     */
    private class CustomMultipartFile(
        private val bytes: ByteArray,
        private val name: String,
        private val originalFilename: String?,
        private val contentType: String?
    ) : MultipartFile {
        override fun getName(): String = name
        override fun getOriginalFilename(): String? = originalFilename
        override fun getContentType(): String? = contentType
        override fun isEmpty(): Boolean = bytes.isEmpty()
        override fun getSize(): Long = bytes.size.toLong()
        override fun getBytes(): ByteArray = bytes
        override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
        override fun transferTo(dest: File): Unit = FileOutputStream(dest).use { it.write(bytes) }
    }

    private data class PostData(
        val member: Member,
        val title: String,
        val description: String,
        val category: Post.Category,
        val price: Int
    )
}
