package com.back.domain.member.controller;

import com.back.domain.auth.dto.request.MemberLoginRequest;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Status;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.rsData.RsData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("MemberController 통합 테스트")
public class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("회원 탈퇴 성공")
    void delete_account_success() throws Exception {
        // given - 테스트용 회원 저장
        String email = "testUser1@user.com";
        String password = "user1234!";
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name("홍길동")
                .build();
        memberRepository.save(member);

        // 로그인 요청
        MemberLoginRequest request = new MemberLoginRequest(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        RsData<Map<String, Object>> rsData = objectMapper.readValue(responseJson,
                new TypeReference<RsData<Map<String, Object>>>() {});
        String accessToken = rsData.data().get("accessToken").toString();

        // when - 탈퇴 요청
        mockMvc.perform(delete("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 탈퇴 성공"));

        // then - 실제 DB 상태 확인
        Member deletedMember = memberRepository.findByEmail(email).orElseThrow();
        assertEquals(Status.DELETED, deletedMember.getStatus());
    }

    @Test
    @DisplayName("마이페이지 조회 성공")
    @WithUserDetails("user1@user.com")
    void myPage_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user1@user.com"))
                .andExpect(jsonPath("$.data.name").value("유저1"));
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 탈퇴한 회원")
    void myPage_fail_deleted_user() throws Exception {
        // given
        if (memberRepository.findByEmail("deleted@user.com").isEmpty()) {
            Member member = Member.builder()
                    .email("deleted@user.com")
                    .password(passwordEncoder.encode("user1234!"))
                    .name("탈퇴자")
                    .status(Status.DELETED)
                    .build();
            memberRepository.save(member);
        }

        // when & then
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 이름만 수정") // 테스트명 변경
    @WithUserDetails(value = "user1@user.com")
    void updateMember_success_nameOnly() throws Exception { // 메서드명 변경
        MemberUpdateRequest request = new MemberUpdateRequest(
                "이름개명",
                null, // profileUrl 필드 제거에 따른 변경
                null
        );

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 정보 수정 성공"));

        Member updated = memberRepository.findByEmail("user1@user.com").orElseThrow();
        assertEquals("이름개명", updated.getName());
        assertNull(updated.getProfileUrl()); // profileUrl이 변경되지 않았음을 확인
    }

//    @Test
//    @DisplayName("회원 정보 수정 성공 - 이름과 프로필 URL 수정")
//    @WithUserDetails(value = "user1@user.com")
//    void updateMember_success_nameAndProfileUrl() throws Exception {
//        MemberUpdateRequest request = new MemberUpdateRequest(
//                "이름개명",
//                "testUrl.com/profile.jpg",
//                null,
//                null
//        );
//
//        mockMvc.perform(patch("/api/members/me")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.resultCode").value("200-7"))
//                .andExpect(jsonPath("$.msg").value("회원 정보 수정에 성공했습니다."));
//
//        Member updated = memberRepository.findByEmail("user1@user.com").orElseThrow();
//        assertEquals("이름개명", updated.getName());
//        assertEquals("testUrl.com/profile.jpg", updated.getProfileUrl());
//    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 비밀번호 수정")
    @WithUserDetails(value = "user1@user.com")
    void updateMember_success_passwordOnly() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                "user1234!",
                "newpass123!"
        );

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 정보 수정 성공"));

        Member updated = memberRepository.findByEmail("user1@user.com").orElseThrow();
        assertTrue(passwordEncoder.matches("newpass123!", updated.getPassword()));
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 현재 비밀번호 불일치")
    @WithUserDetails(value = "user1@user.com")
    void updateMember_fail_wrongCurrentPassword() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                "wrongPassword!",
                "newPassword123!"
        );

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"))
                .andExpect(jsonPath("$.msg").value("현재 비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 현재 비밀번호 누락")
    @WithUserDetails(value = "user1@user.com")
    void updateMember_fail_missingCurrentPassword() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                null,
                "newPassword123!"
        );

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").value("현재 비밀번호를 입력해주세요."));
    }

    @Test
    @DisplayName("타 회원 프로필 조회 성공")
    @WithUserDetails(value = "user1@user.com")
    void getOtherMemberProfile_success() throws Exception {
        // given
        Member otherMember = memberRepository.save(Member.builder()
                .email("other@user.com")
                .password(passwordEncoder.encode("user1234!"))
                .name("다른유저")
                .profileUrl("https://example.com/profile.jpg")
                .status(Status.ACTIVE)
                .build());

        // when & then
        mockMvc.perform(get("/api/members/" + otherMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("사용자 프로필 조회 성공"))
                .andExpect(jsonPath("$.data.name").value("다른유저"))
                .andExpect(jsonPath("$.data.profileUrl").value("https://example.com/profile.jpg"));
    }

    @Test
    @DisplayName("타 회원 프로필 조회 실패 - 존재하지 않는 ID")
    @WithUserDetails(value = "user1@user.com")
    void getOtherMemberProfile_fail_notFound() throws Exception {
        // given
        Long nonExistentId = 99999L;

        // when & then
        mockMvc.perform(get("/api/members/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 사용자가 존재하지 않습니다."));
    }
}
