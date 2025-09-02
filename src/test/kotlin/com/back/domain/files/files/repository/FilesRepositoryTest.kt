package com.back.domain.files.files.repository

import com.back.domain.files.files.entity.Files
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("FilesRepository 테스트")
internal class FilesRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var filesRepository: FilesRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    private lateinit var testMember: Member
    private lateinit var testPost: Post
    private lateinit var testFiles1: Files
    private lateinit var testFiles2: Files
    private lateinit var testFiles3: Files

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성 및 저장
        testMember = memberRepository.save(
            Member(
                email = "test@test.com",
                password = "password123",
                name = "테스트유저",
                profileUrl = null,
                role = Role.USER,
                status = Status.ACTIVE
            )
        )

        // 테스트 게시글 생성 및 저장
        testPost = postRepository.save(
            Post(
                testMember,
                "테스트 게시글",
                "테스트 내용",
                Post.Category.PRODUCT,
                50000,
                Post.Status.SALE
            )
        )

        // 테스트 파일들 생성 및 저장 (sortOrder 순서대로)
        testFiles1 = filesRepository.save(
            Files(
                post = testPost,
                fileName = "first-file.jpg",
                fileType = "image/jpeg",
                fileSize = 1024L,
                fileUrl = "https://example.com/first-file.jpg",
                sortOrder = 1
            )
        )

        testFiles2 = filesRepository.save(
            Files(
                post = testPost,
                fileName = "second-file.pdf",
                fileType = "application/pdf",
                fileSize = 2048L,
                fileUrl = "https://example.com/second-file.pdf",
                sortOrder = 2
            )
        )

        testFiles3 = filesRepository.save(
            Files(
                post = testPost,
                fileName = "third-file.docx",
                fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                fileSize = 4096L,
                fileUrl = "https://example.com/third-file.docx",
                sortOrder = 3
            )
        )

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    internal inner class BasicCrudTest {

        @Test
        @DisplayName("파일 ID로 조회 테스트")
        fun findById() {
            // when
            val foundFile = filesRepository.findById(testFiles1.id)

            // then
            assertThat(foundFile).isPresent
            assertThat(foundFile.get().fileName).isEqualTo("first-file.jpg")
            assertThat(foundFile.get().fileType).isEqualTo("image/jpeg")
            assertThat(foundFile.get().fileSize).isEqualTo(1024L)
        }

        @Test
        @DisplayName("존재하지 않는 파일 ID로 조회 테스트")
        fun findByIdNotFound() {
            // given
            val nonExistentId = 999L

            // when
            val foundFile = filesRepository.findById(nonExistentId)

            // then
            assertThat(foundFile).isEmpty
        }

        @Test
        @DisplayName("파일 저장 테스트")
        fun saveFile() {
            // given
            val newFile = Files(
                post = testPost,
                fileName = "new-file.txt",
                fileType = "text/plain",
                fileSize = 512L,
                fileUrl = "https://example.com/new-file.txt",
                sortOrder = 4
            )

            // when
            val savedFile = filesRepository.save(newFile)

            // then
            assertThat(savedFile.id).isNotNull
            assertThat(savedFile.fileName).isEqualTo("new-file.txt")
            assertThat(savedFile.post.id).isEqualTo(testPost.id)
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    internal inner class CustomQueryTest {

        @Test
        @DisplayName("게시글 ID로 파일 목록 정렬 순서대로 조회")
        fun findByPostIdOrderBySortOrderAsc() {
            // when
            val files = filesRepository.findByPostIdOrderBySortOrderAsc(testPost.id)

            // then
            assertThat(files).hasSize(3)
            assertThat(files[0].fileName).isEqualTo("first-file.jpg")
            assertThat(files[0].sortOrder).isEqualTo(1)
            assertThat(files[1].fileName).isEqualTo("second-file.pdf")
            assertThat(files[1].sortOrder).isEqualTo(2)
            assertThat(files[2].fileName).isEqualTo("third-file.docx")
            assertThat(files[2].sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("Post와 함께 조회 (JOIN FETCH)")
        fun findWithPostByPostId() {
            // when
            val files = filesRepository.findWithPostByPostId(testPost.id)

            // then
            assertThat(files).hasSize(3)
            
            // POST와 함께 조회되었는지 확인 (N+1 문제 방지)
            files.forEach { file ->
                assertThat(file.post).isNotNull
                assertThat(file.post.id).isEqualTo(testPost.id)
                assertThat(file.post.title).isEqualTo("테스트 게시글")
                assertThat(file.post.member).isNotNull
            }

            // 정렬 순서 확인
            assertThat(files).isSortedAccordingTo(compareBy { it.sortOrder })
        }

        @Test
        @DisplayName("마지막 파일 조회 with 비관적 락")
        fun findLastByPostIdWithLock() {
            // given
            val pageable = PageRequest.of(0, 1)

            // when
            val lastFiles = filesRepository.findLastByPostIdWithLock(testPost.id, pageable)

            // then
            assertThat(lastFiles).hasSize(1)
            assertThat(lastFiles[0].fileName).isEqualTo("third-file.docx")
            assertThat(lastFiles[0].sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("존재하지 않는 게시글 ID로 조회")
        fun findByNonExistentPostId() {
            // given
            val nonExistentPostId = 999L

            // when
            val files = filesRepository.findByPostIdOrderBySortOrderAsc(nonExistentPostId)

            // then
            assertThat(files).isEmpty()
        }
    }

    @Nested
    @DisplayName("페이징 및 EntityGraph 테스트")
    internal inner class PagingAndEntityGraphTest {

        @Test
        @DisplayName("EntityGraph를 사용한 페이징 조회")
        fun findAllWithEntityGraph() {
            // given
            val pageable = PageRequest.of(0, 10)

            // when
            val filesPage = filesRepository.findAll(pageable)

            // then
            assertThat(filesPage.content).hasSize(3)
            assertThat(filesPage.totalElements).isEqualTo(3L)
            assertThat(filesPage.totalPages).isEqualTo(1)

            // EntityGraph로 Post가 함께 조회되었는지 확인
            filesPage.content.forEach { file ->
                assertThat(file.post).isNotNull
                assertThat(file.post.title).isNotNull
            }
        }

        @Test
        @DisplayName("페이징 크기 제한 테스트")
        fun findAllWithPagingLimit() {
            // given
            val pageable = PageRequest.of(0, 2)

            // when
            val filesPage = filesRepository.findAll(pageable)

            // then
            assertThat(filesPage.content).hasSize(2)
            assertThat(filesPage.totalElements).isEqualTo(3L)
            assertThat(filesPage.totalPages).isEqualTo(2)
            assertThat(filesPage.hasNext()).isTrue
        }
    }

    @Nested
    @DisplayName("락 테스트")
    internal inner class LockTest {

        @Test
        @DisplayName("비관적 락으로 마지막 파일 조회 시 정렬 테스트")
        fun findLastByPostIdWithLockSorting() {
            // given
            // 정렬 순서를 뒤섞어서 추가 파일 생성
            val additionalFile = filesRepository.save(
                Files(
                    post = testPost,
                    fileName = "additional-file.zip",
                    fileType = "application/zip",
                    fileSize = 8192L,
                    fileUrl = "https://example.com/additional-file.zip",
                    sortOrder = 0 // 가장 낮은 순서
                )
            )

            val pageable = PageRequest.of(0, 2)

            // when
            val lastFiles = filesRepository.findLastByPostIdWithLock(testPost.id, pageable)

            // then
            assertThat(lastFiles).hasSize(2)
            // DESC 정렬이므로 가장 높은 sortOrder가 먼저 와야 함
            assertThat(lastFiles[0].sortOrder).isEqualTo(3)
            assertThat(lastFiles[1].sortOrder).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("연관관계 테스트")
    internal inner class RelationshipTest {

        @Test
        @DisplayName("파일 삭제 시 Post와의 연관관계 확인")
        fun deleteFileAndCheckRelationship() {
            // given
            val initialFileCount = filesRepository.count()

            // when
            filesRepository.delete(testFiles2)
            entityManager.flush()

            // then
            assertThat(filesRepository.count()).isEqualTo(initialFileCount - 1)
            
            // Post는 여전히 존재해야 함
            val post = postRepository.findById(testPost.id)
            assertThat(post).isPresent
        }

        @Test
        @DisplayName("Post 삭제 시 Files cascade 삭제 확인")
        fun deletePostAndCheckFilesCascade() {
            // given
            val initialFileCount = filesRepository.count()
            assertThat(initialFileCount).isEqualTo(3L)

            // when
            postRepository.delete(testPost)
            entityManager.flush()

            // then
            // Post가 삭제되면 연관된 Files도 함께 삭제되어야 함 (cascade = ALL, orphanRemoval = true)
            val remainingFiles = filesRepository.findByPostIdOrderBySortOrderAsc(testPost.id)
            assertThat(remainingFiles).isEmpty()
        }
    }
}
