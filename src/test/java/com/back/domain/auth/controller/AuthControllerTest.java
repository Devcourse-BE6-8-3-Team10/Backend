package com.back.domain.auth.controller;

import com.back.domain.auth.dto.request.MemberLoginRequest;
import com.back.domain.auth.dto.request.MemberSignupRequest;
import com.back.domain.auth.dto.request.TokenReissueRequest;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.entity.Status;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.rsData.ResultCode;
import com.back.global.rsData.RsData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController 통합 테스트")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        MemberSignupRequest request = new MemberSignupRequest("test@example.com", "securePass123!", "홍길동");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))  // ResultCode에 맞게 수정
                .andExpect(jsonPath("$.msg").value("회원가입 성공"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_email_duplicate() throws Exception {
        // given
        Member existing = Member.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("somepass!"))
                .name("기존유저")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
        memberRepository.save(existing);

        MemberSignupRequest request = new MemberSignupRequest("test@example.com", "anotherPass123!", "홍길동");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("로그인 성공(서비스만)")
    void login_success() throws Exception {
        // given
        MemberLoginRequest request = new MemberLoginRequest("user1@user.com", "user1234!");

        // when
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200")) // 로그인 성공 코드
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_invalid_credentials() throws Exception {
        // given
        MemberLoginRequest request = new MemberLoginRequest("user1@user.com", "wrongPassword!");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 잘못되었습니다."));
    }

    @Test
    @DisplayName("JWT 로그인 성공 후 보호된 엔드포인트 접근")
    void login_and_access_protected_endpoint() throws Exception {
        // given
        MemberLoginRequest request = new MemberLoginRequest("user1@user.com", "user1234!");

        // 로그인 요청
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();

        RsData<Map<String, Object>> rsData = objectMapper.readValue(responseJson,
                new TypeReference<RsData<Map<String, Object>>>() {});

        String accessToken = rsData.data().get("accessToken").toString();

        // 보호된 API 접근
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user1@user.com"));
    }

    @Test
    @DisplayName("JWT 로그인 후 로그아웃 성공")
    void login_and_logout_success() throws Exception {
        // given - 로그인 요청
        MemberLoginRequest loginRequest = new MemberLoginRequest("user1@user.com", "user1234!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();

        RsData<Map<String, Object>> loginRsData = objectMapper.readValue(
                loginResponseJson, new TypeReference<RsData<Map<String, Object>>>() {});

        String accessToken = loginRsData.data().get("accessToken").toString();

        // 로그인 직후 refreshToken이 설정되었는지 확인
        Member memberAfterLogin = memberRepository.findByEmail("user1@user.com").orElseThrow();
        assertThat(memberAfterLogin.getRefreshToken()).isNotNull();

        // when - 로그아웃 요청
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String logoutResponseJson = logoutResult.getResponse().getContentAsString();
        RsData<Void> logoutRsData = objectMapper.readValue(
                logoutResponseJson, new TypeReference<RsData<Void>>() {});

        // then - 로그아웃 응답 검증
        assertThat(logoutRsData.resultCode()).isEqualTo(ResultCode.SUCCESS.code());
        assertThat(logoutRsData.msg()).isEqualTo("로그아웃 성공");

        // 로그아웃 후 refreshToken 제거되었는지 확인
        Member member = memberRepository.findByEmail("user1@user.com").orElseThrow();
        assertThat(member.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("AccessToken 재발급 성공")
    void reissueAccessToken_success() throws Exception {
        // given - 로그인 요청
        MemberLoginRequest loginRequest = new MemberLoginRequest("user1@user.com", "user1234!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 응답에서 refreshToken 추출
        String responseJson = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseJson)
                .get("data")
                .get("refreshToken")
                .asText();

        // 재발급 요청 DTO 생성
        TokenReissueRequest reissueRequest = new TokenReissueRequest(refreshToken);

        // when & then - AccessToken 재발급 요청, accessToken, refreshToken 응답
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 유효하지 않은 RefreshToken")
    void reissueAccessToken_fail_invalidToken() throws Exception {
        // given: 존재하지 않거나 유효하지 않은 refreshToken
        TokenReissueRequest reissueRequest = new TokenReissueRequest("invalid-refresh-token");

        // when: 재발급 API 호출
        ResultActions resultActions = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reissueRequest)));

        // then: 401 Unauthorized 응답 및 메시지 검증
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-3"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 토큰 불일치")
    void reissueAccessToken_fail_tokenMismatch() throws Exception {
        // 다른 사용자의 유효한 토큰으로 테스트
        // given - 로그인
        MemberLoginRequest loginRequest = new MemberLoginRequest("user1@user.com", "user1234!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // DB에 저장된 RefreshToken이 아닌 위조된 다른 문자열로 요청
        TokenReissueRequest reissueRequest = new TokenReissueRequest("fake-but-valid-looking-refresh-token");

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-3"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 존재하지 않는 사용자")
    void reissueAccessToken_fail_memberNotFound() throws Exception {
        // 유효한 토큰이지만 사용자가 삭제된 경우
        // given - 토큰 수동 생성 (DB에 없는 사용자 이메일로)
        String invalidEmail = "not@exist.com";
        String fakeToken = Jwts.builder()
                .setSubject(invalidEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1시간
                .signWith(Keys.hmacShaKeyFor("testsecretsecretsecretsecret1234".getBytes()), SignatureAlgorithm.HS256)
                .compact();

        TokenReissueRequest reissueRequest = new TokenReissueRequest(fakeToken);

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-3"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("AccessToken 재발급 후 새로운 RefreshToken으로 재발급 가능")
    void reissueAccessToken_canReissueWithNewToken() throws Exception {
        // 재발급된 새로운 RefreshToken으로 다시 재발급 가능한지 확인
        // 1차 로그인 → refreshToken 발급
        MemberLoginRequest loginRequest = new MemberLoginRequest("user1@user.com", "user1234!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String firstRefreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // 1차 AccessToken 재발급
        TokenReissueRequest firstReissue = new TokenReissueRequest(firstRefreshToken);
        MvcResult reissueResult = mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstReissue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        // 응답에서 새로운 refreshToken 획득
        String newRefreshToken = objectMapper.readTree(reissueResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // 2차 AccessToken 재발급 시도 (새 refreshToken으로)
        TokenReissueRequest secondReissue = new TokenReissueRequest(newRefreshToken);
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReissue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("탈퇴한 회원 로그인 시도 → 실패")
    void login_deleted_user_should_fail() throws Exception {
        // given
        String email = "deleted@user.com";
        String password = "user1234!";
        Member deletedMember = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name("탈퇴자")
                .status(Status.DELETED)
                .build();
        memberRepository.save(deletedMember);

        MemberLoginRequest request = new MemberLoginRequest(email, password);

        // when
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 잘못되었습니다."));
    }
}
