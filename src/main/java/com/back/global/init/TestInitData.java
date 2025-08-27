package com.back.global.init;

import com.back.domain.auth.dto.request.MemberSignupRequest;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.MemberService;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.trade.entity.Trade;
import com.back.domain.trade.entity.TradeStatus;
import com.back.domain.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestInitData {
    @Autowired
    @Lazy
    private TestInitData self;
    private final PostRepository postRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TradeRepository tradeRepository;

    @Bean
    ApplicationRunner testInitDataApplicationRunner() {
        return args -> {
            self.work1();
            self.work2();
            self.work3();
        };
    }

    // 유저&관리자 데이터 삽입
    @Transactional
    public void work1() {
        // 관리자 계정 직접 생성
        if (memberRepository.findByEmail("admin@admin.com").isEmpty()) {
            Member admin = Member.builder()
                    .email("admin@admin.com")
                    .password(passwordEncoder.encode("admin1234!"))
                    .name("관리자")
                    .role(Role.ADMIN)
                    .build();
            memberRepository.save(admin);
        }

        // 일반 유저 계정 생성
        safeSignup("user1@user.com", "user1234!", "유저1");
        safeSignup("user2@user.com", "user1234!", "유저2");
        safeSignup("user3@user.com", "user1234!", "유저3");
        safeSignup("testuser1@user.com", "user1234!", "사용자1");
        safeSignup("testuser2@user.com", "user1234!", "사용자2");
    }

    private void safeSignup(String email, String password, String name) {
        try {
            memberService.signup(new MemberSignupRequest(email, password, name));
        } catch (IllegalArgumentException e) {
            log.debug("Skip creating member ({}): {}", email, e.getMessage());
        }
    }

    // 게시글 데이터 삽입
    @Transactional
    public void work2() {
        Member user1 = memberRepository.findByEmail("user1@user.com")
                .orElseThrow(() -> new RuntimeException("test1@user.com 사용자 없음"));

        Post post1 = Post.builder()
                .member(user1)
                .title("특허1 판매합니다")
                .description("특허1은 이러한 기능입니다")
                .category(Post.Category.METHOD)
                .price(99999)
                .status(Post.Status.SALE)
                .build();

        Post post2 = Post.builder()
                .member(user1)
                .title("특허2 팝니다")
                .description("특허2 기능 설명")
                .category(Post.Category.TRADEMARK)
                .price(10000)
                .status(Post.Status.SALE)
                .build();

        Post post3 = Post.builder()
                .member(user1)
                .title("특허3 판매 완료")
                .description("이미 판매된 특허입니다.")
                .category(Post.Category.METHOD)
                .price(123456)
                .status(Post.Status.SOLD_OUT)
                .build();

        Post tradePost1 = Post.builder()
                .member(user1)
                .title("거래 게시글 A")
                .description("A의 설명")
                .category(Post.Category.COPYRIGHT)
                .price(5000)
                .status(Post.Status.SALE)
                .build();

        Post tradePost2 = Post.builder()
                .member(user1)
                .title("거래 게시글 B")
                .description("B의 설명")
                .category(Post.Category.DESIGN)
                .price(8000)
                .status(Post.Status.SALE)
                .build();

        Post tradePost3 = Post.builder()
                .member(user1)
                .title("거래 게시글 C")
                .description("C의 설명")
                .category(Post.Category.PRODUCT)
                .price(15000)
                .status(Post.Status.SALE)
                .build();

        postRepository.saveAll(List.of(post1, post2, post3, tradePost1, tradePost2, tradePost3));
    }

    // 거래 데이터 삽입
    @Transactional
    public void work3() {
        Member buyer = memberRepository.findByEmail("user2@user.com")
                .orElseThrow(() -> new RuntimeException("user2@user.com 사용자 없음"));
        Member seller = memberRepository.findByEmail("user1@user.com")
                .orElseThrow(() -> new RuntimeException("user1@user.com 사용자 없음"));

        List<Post> tradePosts = postRepository.findAll().stream()
                .filter(p -> p.getTitle().startsWith("거래 게시글"))
                .toList();

        List<Trade> trades = List.of(
                createTrade(tradePosts.get(0), buyer, seller),
                createTrade(tradePosts.get(1), buyer, seller),
                createTrade(tradePosts.get(2), buyer, seller)
        );

        tradeRepository.saveAll(trades);

        // 게시글 상태 변경
        tradePosts.forEach(Post::markAsSoldOut);
    }

    private Trade createTrade(Post post, Member buyer, Member seller) {
        return Trade.builder()
                .post(post)
                .buyer(buyer)
                .seller(seller)
                .price(post.getPrice())
                .status(TradeStatus.COMPLETED)
                .build();
    }
}
