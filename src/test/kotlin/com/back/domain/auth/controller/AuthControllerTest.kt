package com.back.domain.auth.controller

import com.back.domain.auth.dto.request.MemberLoginRequest
import com.back.domain.auth.dto.request.MemberSignupRequest
import com.back.domain.auth.dto.request.TokenReissueRequest
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Test
    @DisplayName("회원가입 성공")
    fun signup_success() {
        // given
        val request = MemberSignupRequest("test@example.com", "securePass123!", "홍길동")

        // when & then
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("회원가입 성공"))
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    fun signup_email_duplicate() {
        // given
        val existing = Member(
            "test@example.com",
            passwordEncoder.encode("somepass!"),
            "기존유저",
            null,
            Role.USER,
            Status.ACTIVE
        )
        memberRepository.save(existing)

        val request = MemberSignupRequest("test@example.com", "anotherPass123!", "홍길동")

        // when & then
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-2"))
            .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."))
    }

    @Test
    @DisplayName("로그인 성공(서비스만)")
    fun login_success() {
        // given
        val request = MemberLoginRequest("user1@user.com", "user1234!")

        // when
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn()
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    fun login_invalid_credentials() {
        // given
        val request = MemberLoginRequest("user1@user.com", "wrongPassword!")

        // when & then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
            .andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 잘못되었습니다."))
    }

    @Test
    @DisplayName("JWT 로그인 성공 후 보호된 엔드포인트 접근")
    fun login_and_access_protected_endpoint() {
        // given
        val request = MemberLoginRequest("user1@user.com", "user1234!")

        // 로그인 요청
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = loginResult.response.contentAsString
        val rsData = objectMapper.readValue(
            responseJson,
            object : TypeReference<RsData<Map<String, Any>>>() {}
        )
        val accessToken = rsData.data?.get("accessToken").toString()

        // 보호된 API 접근
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("user1@user.com"))
    }

    @Test
    @DisplayName("JWT 로그인 후 로그아웃 성공")
    fun login_and_logout_success() {
        // given - 로그인 요청
        val loginRequest = MemberLoginRequest("user1@user.com", "user1234!")

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponseJson = loginResult.response.contentAsString
        val loginRsData = objectMapper.readValue(
            loginResponseJson,
            object : TypeReference<RsData<Map<String, Any>>>() {}
        )
        val accessToken = loginRsData.data?.get("accessToken").toString()

        // 로그인 직후 refreshToken이 설정되었는지 확인
        val memberAfterLogin = memberRepository.findByEmail("user1@user.com").orElseThrow()
        assertThat(memberAfterLogin.refreshToken).isNotNull()

        // when - 로그아웃 요청
        val logoutResult = mockMvc.perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andReturn()

        val logoutResponseJson = logoutResult.response.contentAsString
        val logoutRsData = objectMapper.readValue(
            logoutResponseJson,
            object : TypeReference<RsData<Void>>() {}
        )

        // then - 로그아웃 응답 검증
        assertThat(logoutRsData.resultCode).isEqualTo(ResultCode.SUCCESS.code())
        assertThat(logoutRsData.msg).isEqualTo("로그아웃 성공")

        // 로그아웃 후 refreshToken 제거되었는지 확인
        val member = memberRepository.findByEmail("user1@user.com").orElseThrow()
        assertThat(member.refreshToken).isNull()
    }

    @Test
    @DisplayName("AccessToken 재발급 성공")
    fun reissueAccessToken_success() {
        // given - 로그인 요청
        val loginRequest = MemberLoginRequest("user1@user.com", "user1234!")

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        // 응답에서 refreshToken 추출
        val responseJson = loginResult.response.contentAsString
        val refreshToken = objectMapper.readTree(responseJson)
            .get("data")
            .get("refreshToken")
            .asText()

        // 재발급 요청 DTO 생성
        val reissueRequest = TokenReissueRequest(refreshToken)

        // when & then - AccessToken 재발급 요청, accessToken, refreshToken 응답
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reissueRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 유효하지 않은 RefreshToken")
    fun reissueAccessToken_fail_invalidToken() {
        // given: 존재하지 않거나 유효하지 않은 refreshToken
        val reissueRequest = TokenReissueRequest("invalid-refresh-token")

        // when & then: 401 Unauthorized 응답 및 메시지 검증
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reissueRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-3"))
            .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."))
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 토큰 불일치")
    fun reissueAccessToken_fail_tokenMismatch() {
        // 다른 사용자의 유효한 토큰으로 테스트
        // given - 로그인
        val loginRequest = MemberLoginRequest("user1@user.com", "user1234!")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)

        // DB에 저장된 RefreshToken이 아닌 위조된 다른 문자열로 요청
        val reissueRequest = TokenReissueRequest("fake-but-valid-looking-refresh-token")

        // when & then
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reissueRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-3"))
            .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."))
    }

    @Test
    @DisplayName("AccessToken 재발급 실패 - 존재하지 않는 사용자")
    fun reissueAccessToken_fail_memberNotFound() {
        // 유효한 토큰이지만 사용자가 삭제된 경우
        // given - 토큰 수동 생성 (DB에 없는 사용자 이메일로)
        val invalidEmail = "not@exist.com"
        val fakeToken = Jwts.builder()
            .setSubject(invalidEmail)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1시간
            .signWith(Keys.hmacShaKeyFor("testsecretsecretsecretsecret1234".toByteArray()), SignatureAlgorithm.HS256)
            .compact()

        val reissueRequest = TokenReissueRequest(fakeToken)

        // when & then
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reissueRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-3"))
            .andExpect(jsonPath("$.msg").value("유효하지 않은 리프레시 토큰입니다."))
    }

    @Test
    @DisplayName("AccessToken 재발급 후 새로운 RefreshToken으로 재발급 가능")
    fun reissueAccessToken_canReissueWithNewToken() {
        // 재발급된 새로운 RefreshToken으로 다시 재발급 가능한지 확인
        // 1차 로그인 → refreshToken 발급
        val loginRequest = MemberLoginRequest("user1@user.com", "user1234!")
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val firstRefreshToken = objectMapper.readTree(loginResult.response.contentAsString)
            .get("data")
            .get("refreshToken")
            .asText()

        // 1차 AccessToken 재발급
        val firstReissue = TokenReissueRequest(firstRefreshToken)
        val reissueResult = mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstReissue))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn()

        // 응답에서 새로운 refreshToken 획득
        val newRefreshToken = objectMapper.readTree(reissueResult.response.contentAsString)
            .get("data")
            .get("refreshToken")
            .asText()

        // 2차 AccessToken 재발급 시도 (새 refreshToken으로)
        val secondReissue = TokenReissueRequest(newRefreshToken)
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondReissue))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    @DisplayName("탈퇴한 회원 로그인 시도 → 실패")
    fun login_deleted_user_should_fail() {
        // given
        val email = "deleted@user.com"
        val password = "user1234!"
        val deletedMember = Member(
            email,
            passwordEncoder.encode(password),
            "탈퇴자",
            null,
            Role.USER,
            Status.DELETED
        )
        memberRepository.save(deletedMember)

        val request = MemberLoginRequest(email, password)

        // when & then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
            .andExpect(jsonPath("$.msg").value("탈퇴한 회원입니다."))
    }
}
