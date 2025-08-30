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

        // 페이징 적용 및 DTO 변환하여 반환
        return memberRepository.findAllByRoleNot(Role.ADMIN, pageable)
                .map(AdminMemberResponse::fromEntity);
    }

    // 회원 상세 조회
    public AdminMemberResponse getMemberDetail(Long memberId) {
        log.info("회원 상세 조회 요청 - memberId: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "존재하지 않는 회원입니다."));

        return AdminMemberResponse.fromEntity(member);
    }

    // 회원 정보 수정
    @Transactional
    public void updateMemberInfo(Long memberId, AdminUpdateMemberRequest request){
        log.info("회원 정보 수정 요청 - memberId: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "해당 회원이 존재하지 않습니다."));

        // 1. 이름 변경 (@Valid로 검증되므로 null/blank 체크 불필요)
        if (request.getName() != null) {
            member.updateName(request.getName());
        }

        // 2. 상태 변경
        if (request.getStatus() != null) {
            member.changeStatus(request.getStatus());
        }

        // 3. 프로필 이미지 변경
        if (request.getProfileUrl() != null) {
            String profileUrl = request.getProfileUrl().trim().isEmpty() ? null : request.getProfileUrl();
            member.updateProfileUrl(profileUrl);
        }

        memberRepository.save(member);
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

        // DTO에서 이미 enum 타입으로 받았으므로 직접 사용
        Post.Category category = request.getCategory();
        Post.Status status = request.getStatus();

        // 엔티티 업데이트
        post.updatePost(request.getTitle(), request.getDescription(), category, request.getPrice());
        post.updateStatus(status);

        postRepository.save(post);
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

        // 1. 반드시 영속 상태로 다시 가져오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "해당 회원이 존재하지 않습니다."));

        // 2. 회원 탈퇴 처리
        member.delete();
        memberRepository.save(member);
    }
}
