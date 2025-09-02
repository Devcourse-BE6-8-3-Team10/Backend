package com.back.global.init

import com.back.domain.auth.dto.request.MemberSignupRequest
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.repository.MemberRepository
import com.back.domain.member.service.MemberService
import com.back.domain.post.entity.Post
import com.back.domain.post.repository.PostRepository
import com.back.domain.trade.entity.Trade
import com.back.domain.trade.entity.TradeStatus
import com.back.domain.trade.repository.TradeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Configuration
@Profile("test")
class TestInitData(
    private val postRepository: PostRepository,
    private val memberService: MemberService,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tradeRepository: TradeRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 자기 자신을 프록시로 주입해 @Transactional 메서드 내부호출도 트랜잭션이 적용되도록
    @Autowired @Lazy
    private lateinit var self: TestInitData

    @Bean
    fun testInitDataApplicationRunner() = ApplicationRunner {
        self.work1()
        self.work2()
        self.work3()
    }

    // 유저&관리자 데이터 삽입
    @Transactional
    fun work1() {
        // 관리자 계정 직접 생성
        if (memberRepository.findByEmail("admin@admin.com").isEmpty) {
            val admin = Member(
                "admin@admin.com",
                passwordEncoder.encode("admin1234!"),
                "관리자",
                null,
                Role.ADMIN
            )
            memberRepository.save(admin)
        }

        // 일반 유저 계정 생성
        safeSignup("user1@user.com", "user1234!", "유저1")
        safeSignup("user2@user.com", "user1234!", "유저2")
        safeSignup("user3@user.com", "user1234!", "유저3")
        safeSignup("testuser1@user.com", "user1234!", "사용자1")
        safeSignup("testuser2@user.com", "user1234!", "사용자2")
    }

    private fun safeSignup(email: String, password: String, name: String) {
        runCatching {
            memberService.signup(MemberSignupRequest(email, password, name))
        }.onFailure { e ->
            // 이미 존재 등 예외 시 로그만 남기고 스킵
            log.debug("Skip creating member ({}): {}", email, e.message)
        }
    }

    // 게시글 데이터 삽입
    @Transactional
    fun work2() {
        val user1 = memberRepository.findByEmail("user1@user.com")
            .orElseThrow { RuntimeException("user1@user.com 사용자 없음") }

        val post1 = Post(
            user1,
            "특허1 판매합니다",
            "특허1은 이러한 기능입니다",
            Post.Category.METHOD,
            99999,
            Post.Status.SALE
        )

        val post2 = Post(
            user1,
            "특허2 팝니다",
            "특허2 기능 설명",
            Post.Category.TRADEMARK,
            10000,
            Post.Status.SALE
        )

        val post3 = Post(
            user1,
            "특허3 판매 완료",
            "이미 판매된 특허입니다.",
            Post.Category.METHOD,
            123456,
            Post.Status.SOLD_OUT
        )

        val tradePost1 = Post(
            user1,
            "거래 게시글 A",
            "A의 설명",
            Post.Category.COPYRIGHT,
            5000,
            Post.Status.SALE
        )

        val tradePost2 = Post(
            user1,
            "거래 게시글 B",
            "B의 설명",
            Post.Category.DESIGN,
            8000,
            Post.Status.SALE
        )

        val tradePost3 = Post(
            user1,
            "거래 게시글 C",
            "C의 설명",
            Post.Category.PRODUCT,
            15000,
            Post.Status.SALE
        )

        postRepository.saveAll(listOf(post1, post2, post3, tradePost1, tradePost2, tradePost3))
    }

    // 거래 데이터 삽입
    @Transactional
    fun work3() {
        val buyer = memberRepository.findByEmail("user2@user.com")
            .orElseThrow { RuntimeException("user2@user.com 사용자 없음") }
        val seller = memberRepository.findByEmail("user1@user.com")
            .orElseThrow { RuntimeException("user1@user.com 사용자 없음") }

        val tradePosts = postRepository.findAll()
            .filter { p: Post -> p.title.startsWith("거래 게시글") }


        val trades = listOf(
            createTrade(tradePosts.get(0), buyer, seller),
            createTrade(tradePosts.get(1), buyer, seller),
            createTrade(tradePosts.get(2), buyer, seller)
        )

        tradeRepository.saveAll(trades)

        // 게시글 상태 변경
        tradePosts.forEach { it.markAsSoldOut() }
    }

    private fun createTrade(post: Post, buyer: Member, seller: Member): Trade =
        Trade(post, seller, buyer, post.price, TradeStatus.COMPLETED)
}

