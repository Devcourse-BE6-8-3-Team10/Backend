package com.back.domain.member.service;


import com.back.domain.auth.dto.request.MemberSignupRequest;
import com.back.domain.files.files.service.FileStorageService;
import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.dto.request.FindPasswordRequest;
import com.back.domain.member.dto.response.MemberMyPageResponse;
import com.back.domain.member.dto.response.OtherMemberInfoResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;


    // 회원 가입
    @Transactional
    public void signup(MemberSignupRequest request) {

        // 1. 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email())) {
            throw new ServiceException(ResultCode.DUPLICATE_EMAIL.code(), "이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        memberRepository.save(member);
    }

    // 회원 탈퇴 (상태 변경)
    @Transactional
    public void deleteAccount(Member member) {
        // 1. 반드시 영속 상태로 다시 가져오기
        Member foundMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원 정보가 존재하지 않습니다."));

        // 2. 회원 탈퇴 처리
        foundMember.delete();
        memberRepository.save(foundMember);
    }

    // 회원 마이페이지 조회
    public MemberMyPageResponse findMyPage(Member member) {
        // 1. 반드시 영속 상태로 다시 가져오기
        Member foundMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원 정보가 존재하지 않습니다."));

        // 2. 마이페이지 정보 반환
        return MemberMyPageResponse.fromEntity(foundMember);
    }

    // 회원 정보 수정
    @Transactional
    public void updateMemberInfo(Member member, MemberUpdateRequest request) {
        // 1. 반드시 영속 상태로 다시 가져오기
        Member foundMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원 정보가 존재하지 않습니다."));

        // 2. 이름 변경
        if (request.name() != null && !request.name().isBlank()) {
            foundMember.updateName(request.name());
        }

//        // 3. 프로필 URL 변경
//        if (request.profileUrl() != null && !request.profileUrl().isBlank()) {
//            foundMember.updateProfileUrl(request.profileUrl());
//        }

        // 4. 비밀번호 변경 요청이 있을 경우만 현재 비밀번호 확인
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new ServiceException(ResultCode.BAD_REQUEST.code(), "현재 비밀번호를 입력해주세요.");
            }

            if (!passwordEncoder.matches(request.currentPassword(), foundMember.getPassword())) {
                throw new ServiceException(ResultCode.INVALID_PASSWORD.code(), "현재 비밀번호가 일치하지 않습니다.");
            }

            foundMember.updatePassword(passwordEncoder.encode(request.newPassword()));
        }
        memberRepository.save(foundMember);
    }

    // 사용자 프로필 조회
    public OtherMemberInfoResponse getMemberProfileById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "해당 사용자가 존재하지 않습니다."));
        return OtherMemberInfoResponse.fromEntity(member);
    }


    // 프로필 이미지 등록 및 업데이트
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        // file이 null이거나 비어있는 경우 예외 처리
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.BAD_REQUEST.code(), "업로드할 파일이 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원을 찾을 수 없습니다."));

        // 기존 프로필 이미지가 있다면 삭제
        String oldProfileUrl = member.getProfileUrl();
        if (oldProfileUrl != null && !oldProfileUrl.isEmpty()) {
            try {
                // 이전 프로필 이미지 삭제 시, 전체 URL을 넘겨야 함 (FileStorageService의 deletePhysicalFile 구현에 따라)
                fileStorageService.deletePhysicalFile(oldProfileUrl);
            } catch (Exception e) {
                log.warn("기존 프로필 이미지 삭제 실패: {}", oldProfileUrl, e);
            }
        }

        try {
            // MemberService에서 파일을 저장할 때, 'profile/{memberId}'를 하위 폴더로 지정
            String newProfileUrl = fileStorageService.storeFile(file, "profile/" + memberId); // 수정된 부분
            member.updateProfileUrl(newProfileUrl);
            memberRepository.save(member);
            return newProfileUrl;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.FILE_UPLOAD_FAIL.code(), "프로필 이미지 업로드에 실패했습니다.");
        }
    }

    // 프로필 이미지 삭제
    @Transactional
    public void deleteProfileImage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원을 찾을 수 없습니다."));

        if (member.getProfileUrl() == null || member.getProfileUrl().isEmpty()) {
            throw new ServiceException(ResultCode.BAD_REQUEST.code(), "삭제할 프로필 이미지가 없습니다.");
        }

        try{
            fileStorageService.deletePhysicalFile(member.getProfileUrl()); // FileStorageService를 사용하여 파일 삭제
            member.updateProfileUrl(null); // Member 엔티티의 profileUrl null로 설정
            memberRepository.save(member); // 변경사항 저장
        } catch (Exception e) {
            throw new ServiceException(ResultCode.FILE_DELETE_FAIL.code(), "프로필 이미지 삭제 실패.");
        }
    }

    // 특정 회원의 프로필 이미지 URL 조회 (별도 메서드로도 제공 가능)
    public String getProfileImageUrl(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "회원을 찾을 수 없습니다."));
        return member.getProfileUrl();
    }

    // 회원 확인 (비밀번호 찾기용)
    public void verifyMember(FindPasswordRequest request) {
        // 이름과 이메일로 회원 존재 여부만 확인 (더 빠른 쿼리)
        boolean exists = memberRepository.existsByNameAndEmail(request.name(), request.email());
        if (!exists) {
            throw new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "해당 정보와 일치하는 회원이 없습니다.");
        }
    }

    // 비밀번호 찾기 및 변경
    @Transactional
    public void findAndUpdatePassword(FindPasswordRequest request) {
        // 1. 이름과 이메일로 회원 찾기
        Member member = memberRepository.findByNameAndEmail(request.name(), request.email())
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "해당 정보와 일치하는 회원이 없습니다."));

        // 2. 새 비밀번호와 확인 비밀번호가 제공되었는지 확인
        if (request.newPassword() == null || request.newPassword().isBlank() || 
            request.confirmPassword() == null || request.confirmPassword().isBlank()) {
            throw new ServiceException(ResultCode.BAD_REQUEST.code(), "새 비밀번호와 확인 비밀번호를 모두 입력해주세요.");
        }

        // 3. 새 비밀번호와 확인 비밀번호 일치 여부 확인
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ServiceException(ResultCode.BAD_REQUEST.code(), "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 4. 새 비밀번호로 업데이트
        member.updatePassword(passwordEncoder.encode(request.newPassword()));
        memberRepository.save(member);
    }
    
}