package com.back.domain.admin.service;

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest;
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest;
import com.back.domain.admin.dto.response.AdminMemberResponse;
import com.back.domain.admin.dto.response.AdminPatentResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    // 전체 회원 목록 조회(관리자 제외)
    public Page<AdminMemberResponse> getAllMembers(Pageable pageable) {
        log.info("전체 회원 목록 조회 요청 (관리자 제외)");

        return memberRepository.findAllByRoleNot(Role.ADMIN, pageable)
                .map(AdminMemberResponse::fromEntity);
    }

    // 회원 상세 조회
    public AdminMemberResponse getMemberDetail(Long memberId) {
        log.info("회원 상세 조회 요청 - memberId: {}", memberId);
        
        Member member = findMemberById(memberId);
        return AdminMemberResponse.fromEntity(member);
    }

    // 회원 정보 수정
    @Transactional
    public void updateMemberInfo(Long memberId, AdminUpdateMemberRequest request) {
        log.info("회원 정보 수정 요청 - memberId: {}", memberId);

        Member member = findMemberById(memberId);

        try {
            member.updateName(request.getName());
            member.changeStatus(request.getStatus());
            updateProfileUrlIfPresent(member, request.getProfileUrl());
        } catch (IllegalStateException e) {
            throw new ServiceException("400", e.getMessage());
        }
    }

    // 전체 특허 목록 조회
    public Page<AdminPatentResponse> getAllPatents(Pageable pageable) {
        log.info("전체 특허 목록 조회 요청");
        return postRepository.findAll(pageable)
                .map(AdminPatentResponse::fromEntity);
    }

    // 특허 상세 조회
    public AdminPatentResponse getPatentDetail(Long patentId) {
        log.info("특허 상세 조회 요청 - patentId: {}", patentId);
        Post post = postRepository.findById(patentId)
                .orElseThrow(() -> new ServiceException(ResultCode.POST_NOT_FOUND.code(), "존재하지 않는 특허입니다."));

        return AdminPatentResponse.fromEntity(post);
    }

    // 특허 정보 수정 (DTO 유효성 검사로 간소화)
    @Transactional
    public void updatePatentInfo(Long patentId, AdminUpdatePatentRequest request) {
        log.info("특허 정보 수정 요청 - patentId: {}", patentId);

        Post post = postRepository.findById(patentId)
                .orElseThrow(() -> new ServiceException(ResultCode.POST_NOT_FOUND.code(), "해당 특허가 존재하지 않습니다."));

        // 엔티티 업데이트
        post.updatePost(request.getTitle(), request.getDescription(), request.getCategory(), request.getPrice());
        post.updateStatus(request.getStatus());
    }

    // 특허 삭제
    @Transactional
    public void deletePatent(Long patentId) {
        log.info("특허 삭제 요청 - patentId: {}", patentId);

        Post post = postRepository.findById(patentId)
                .orElseThrow(() -> new ServiceException(ResultCode.POST_NOT_FOUND.code(), "해당 특허가 존재하지 않습니다."));

        postRepository.delete(post);
    }

    // 회원 탈퇴 (관리자)
    @Transactional
    public void deleteMember(Long memberId) {
        log.info("회원 탈퇴 요청 - memberId: {}", memberId);

        Member member = findMemberById(memberId);
        member.delete();
    }

    // === 헬퍼 메서드들 ===
    
    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "존재하지 않는 회원입니다."));
    }
    
    private void updateProfileUrlIfPresent(Member member, String profileUrl) {
        if (StringUtils.hasText(profileUrl)) {
            member.updateProfileUrl(profileUrl.trim());
        } else if (profileUrl != null) {
            // 빈 문자열인 경우 null로 설정
            member.updateProfileUrl(null);
        }
        // profileUrl이 null인 경우 아무것도 하지 않음 (기존 값 유지)
    }
}
