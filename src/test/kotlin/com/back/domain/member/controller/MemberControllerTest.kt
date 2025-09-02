package com.back.domain.member.controller

import com.back.domain.auth.dto.request.MemberLoginRequest
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.dto.request.MemberUpdateRequest
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import com.back.domain.member.repository.MemberRepository
import com.back.global.rsData.RsData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("MemberController 통합 테스트")
class MemberControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var entityManager: EntityManager

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Test
    @DisplayName("회원 탈퇴 성공")
    fun delete_account_success() {
        // given - 테스트용 회원 저장
        val email = "testUser1@user.com"
        val password = "user1234!"
        val member = Member(
            email,
            passwordEncoder.encode(password),
            "홍길동"
        )
        memberRepository.save(member)

        // 로그인 요청
        val request = MemberLoginRequest(email, password)

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

        // when - 탈퇴 요청
        mockMvc.perform(
            delete("/api/members/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("회원 탈퇴 성공"))

        // then - 실제 DB 상태 확인
        val deletedMember = memberRepository.findByEmail(email).orElseThrow()
        assertEquals(Status.DELETED, deletedMember.status)
    }

    @Test
    @DisplayName("마이페이지 조회 성공")
    @WithUserDetails("user1@user.com")
    fun myPage_success() {
        // when & then
        mockMvc.perform(get("/api/members/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("user1@user.com"))
            .andExpect(jsonPath("$.data.name").value("유저1"))
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 탈퇴한 회원")
    fun myPage_fail_deleted_user() {
        // given
        if (memberRepository.findByEmail("deleted@user.com").isEmpty) {
            val member = Member(
                "deleted@user.com",
                passwordEncoder.encode("user1234!"),
                "탈퇴자",
                null,
                Role.USER,
                Status.DELETED
            )
            memberRepository.save(member)
        }

        // when & then
        mockMvc.perform(get("/api/members/me"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 이름만 수정")
    @WithUserDetails(value = "user1@user.com")
    fun updateMember_success_nameOnly() {
        val request = MemberUpdateRequest(
            "이름개명",
            null,
            null
        )

        mockMvc.perform(
            patch("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("회원 정보 수정 성공"))

        val updated = memberRepository.findByEmail("user1@user.com").orElseThrow()
        assertEquals("이름개명", updated.name)
        assertNull(updated.profileUrl)
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 비밀번호 수정")
    @WithUserDetails(value = "user1@user.com")
    fun updateMember_success_passwordOnly() {
        val request = MemberUpdateRequest(
            null,
            "user1234!",
            "newpass123!"
        )

        mockMvc.perform(
            patch("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("회원 정보 수정 성공"))

        val updated = memberRepository.findByEmail("user1@user.com").orElseThrow()
        assertTrue(passwordEncoder.matches("newpass123!", updated.password))
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 현재 비밀번호 불일치")
    @WithUserDetails(value = "user1@user.com")
    fun updateMember_fail_wrongCurrentPassword() {
        val request = MemberUpdateRequest(
            null,
            "wrongPassword!",
            "newPassword123!"
        )

        mockMvc.perform(
            patch("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-3"))
            .andExpect(jsonPath("$.msg").value("현재 비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 현재 비밀번호 누락")
    @WithUserDetails(value = "user1@user.com")
    fun updateMember_fail_missingCurrentPassword() {
        val request = MemberUpdateRequest(
            null,
            null,
            "newPassword123!"
        )

        mockMvc.perform(
            patch("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400"))
            .andExpect(jsonPath("$.msg").value("현재 비밀번호를 입력해주세요."))
    }

    @Test
    @DisplayName("타 회원 프로필 조회 성공")
    @WithUserDetails(value = "user1@user.com")
    fun getOtherMemberProfile_success() {
        // given
        val otherMember = memberRepository.save(
            Member(
                "other@user.com",
                passwordEncoder.encode("user1234!"),
                "다른유저",
                "https://example.com/profile.jpg"
            )
        )

        // when & then
        mockMvc.perform(get("/api/members/${otherMember.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("사용자 프로필 조회 성공"))
            .andExpect(jsonPath("$.data.name").value("다른유저"))
            .andExpect(jsonPath("$.data.profileUrl").value("https://example.com/profile.jpg"))
    }

    @Test
    @DisplayName("타 회원 프로필 조회 실패 - 존재하지 않는 ID")
    @WithUserDetails(value = "user1@user.com")
    fun getOtherMemberProfile_fail_notFound() {
        // given
        val nonExistentId = 99999L

        // when & then
        mockMvc.perform(get("/api/members/$nonExistentId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("해당 사용자가 존재하지 않습니다."))
    }
}
