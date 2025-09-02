package com.back.domain.member.service

import com.back.domain.auth.dto.request.MemberSignupRequest
import com.back.domain.files.files.service.FileStorageService
import com.back.domain.member.dto.request.FindPasswordRequest
import com.back.domain.member.dto.request.MemberUpdateRequest
import com.back.domain.member.dto.response.MemberMyPageResponse
import com.back.domain.member.dto.response.OtherMemberInfoResponse
import com.back.domain.member.entity.Member
import com.back.domain.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.ResultCode
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val fileStorageService: FileStorageService
) {
    companion object {
        private val log = LoggerFactory.getLogger(MemberService::class.java)
    }

    // 회원 가입
    @Transactional
    fun signup(request: MemberSignupRequest) {
        // 1. 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email)) {
            throw ServiceException(ResultCode.BAD_REQUEST.code, "이미 사용 중인 이메일입니다.")
        }

        val member = Member(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name
        )

        memberRepository.save(member)
    }

    // 회원 탈퇴 (상태 변경)
    @Transactional
    fun deleteAccount(member: Member) {
        // 1. 반드시 영속 상태로 다시 가져오기
        val foundMember = memberRepository.findById(member.id)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원 정보가 존재하지 않습니다."
                )
            }

        // 2. 회원 탈퇴 처리
        foundMember.delete()
        memberRepository.save(foundMember)
    }

    // 회원 마이페이지 조회
    fun findMyPage(member: Member): MemberMyPageResponse {
        // 1. 반드시 영속 상태로 다시 가져오기
        val foundMember = memberRepository.findById(member.id)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원 정보가 존재하지 않습니다."
                )
            }

        // 2. 마이페이지 정보 반환
        return MemberMyPageResponse.fromEntity(foundMember)
    }

    // 회원 정보 수정
    @Transactional
    fun updateMemberInfo(member: Member, request: MemberUpdateRequest) {
        // 1. 반드시 영속 상태로 다시 가져오기
        val foundMember = memberRepository.findById(member.id)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원 정보가 존재하지 않습니다."
                )
            }

        // 2. 이름 변경
        request.name?.takeIf { it.isNotBlank() }?.let { newName ->
            foundMember.updateName(newName)
        }

        // 3. 비밀번호 변경 요청이 있을 경우만 현재 비밀번호 확인
        request.newPassword?.takeIf { it.isNotBlank() }?.let { newPassword ->
            val currentPassword = request.currentPassword
                ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "현재 비밀번호를 입력해주세요.")

            if (currentPassword.isBlank()) {
                throw ServiceException(ResultCode.BAD_REQUEST.code, "현재 비밀번호를 입력해주세요.")
            }

            if (!passwordEncoder.matches(currentPassword, foundMember.password)) {
                throw ServiceException(ResultCode.BAD_REQUEST.code, "현재 비밀번호가 일치하지 않습니다.")
            }

            foundMember.updatePassword(passwordEncoder.encode(newPassword))
        }

        memberRepository.save(foundMember)
    }

    // 사용자 프로필 조회
    fun getMemberProfileById(id: Long): OtherMemberInfoResponse {
        val member = memberRepository.findById(id)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "해당 사용자가 존재하지 않습니다."
                )
            }
        return OtherMemberInfoResponse.fromEntity(member)
    }

    // 프로필 이미지 등록 및 업데이트
    @Transactional
    fun uploadProfileImage(memberId: Long, file: MultipartFile): String {
        // file이 null이거나 비어있는 경우 예외 처리
        if (file.isEmpty) {
            throw ServiceException(ResultCode.BAD_REQUEST.code, "업로드할 파일이 없습니다.")
        }

        val member = memberRepository.findById(memberId)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원을 찾을 수 없습니다."
                )
            }

        // 기존 프로필 URL 보관 (신규 저장 성공 후 삭제)
        val oldProfileUrl = member.profileUrl

        return try {
            val fileContent = file.bytes
            val originalFilename = file.originalFilename
                ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "파일명이 없습니다.")
            val contentType = file.contentType
                ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "파일 타입을 확인할 수 없습니다.")

            // 파일 검증
            val allowed = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
            if (contentType !in allowed) {
                throw ServiceException(ResultCode.BAD_REQUEST.code, "이미지 파일만 업로드 가능합니다.")
            }
            val maxSize = 5 * 1024 * 1024L // 5MB
            if (file.size > maxSize) {
                throw ServiceException(ResultCode.BAD_REQUEST.code, "파일 크기가 5MB를 초과합니다.")
            }
            val cleanedFilename = StringUtils.cleanPath(originalFilename)

            // MemberService에서 파일을 저장할 때, 'profile/{memberId}'를 하위 폴더로 지정
            val newProfileUrl = fileStorageService.storeFile(
                fileContent,
                cleanedFilename,
                contentType,
                "profile/$memberId"
            )

            member.updateProfileUrl(newProfileUrl)
            memberRepository.save(member)

            // 신규 저장 성공 후 구파일 삭제 (실패해도 작업 계속)
            oldProfileUrl?.takeIf { it.isNotBlank() }?.let {
                try {
                    fileStorageService.deletePhysicalFile(it)
                } catch (e: Exception) {
                    log.warn("Old profile deletion failed. memberId={}, url={}", memberId, it, e)
                }
            }
            newProfileUrl
        } catch (e: IOException) {
            log.error("IOException while processing profile image. memberId={}", memberId, e)
            throw ServiceException(ResultCode.INTERNAL_ERROR.code, "프로필 이미지 처리 중 오류가 발생했습니다.")
        } catch (e: Exception) {
            log.error("Unexpected error during profile upload. memberId={}", memberId, e)
            throw ServiceException(ResultCode.FILE_PROCESSING_ERROR.code, "프로필 이미지 업로드에 실패했습니다.")
        }
    }

    // 프로필 이미지 삭제
    @Transactional
    fun deleteProfileImage(memberId: Long) {
        val member = memberRepository.findById(memberId)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원을 찾을 수 없습니다."
                )
            }

        val profileUrl = member.profileUrl
        if (profileUrl.isNullOrEmpty()) {
            throw ServiceException(ResultCode.BAD_REQUEST.code, "삭제할 프로필 이미지가 없습니다.")
        }

        try {
            fileStorageService.deletePhysicalFile(profileUrl)
            member.updateProfileUrl(null)
            memberRepository.save(member)
        } catch (e: Exception) {
            throw ServiceException(ResultCode.FILE_PROCESSING_ERROR.code, "프로필 이미지 삭제 실패.")
        }
    }

    // 특정 회원의 프로필 이미지 URL 조회
    fun getProfileImageUrl(memberId: Long): String? {
        val member = memberRepository.findById(memberId)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "회원을 찾을 수 없습니다."
                )
            }
        return member.profileUrl
    }

    // 회원 확인 (비밀번호 찾기용)
    fun verifyMember(request: FindPasswordRequest) {
        // 이름과 이메일 null 체크
        val name = request.name ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "이름을 입력해주세요.")
        val email = request.email ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "이메일을 입력해주세요.")

        // 이름과 이메일로 회원 존재 여부만 확인 (더 빠른 쿼리)
        val exists = memberRepository.existsByNameAndEmail(name, email)
        if (!exists) {
            throw ServiceException(ResultCode.MEMBER_NOT_FOUND.code, "해당 정보와 일치하는 회원이 없습니다.")
        }
    }

    // 비밀번호 찾기 및 변경
    @Transactional
    fun findAndUpdatePassword(request: FindPasswordRequest) {
        // 1. 이름과 이메일 null 체크
        val name = request.name ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "이름을 입력해주세요.")
        val email = request.email ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "이메일을 입력해주세요.")

        // 2. 이름과 이메일로 회원 찾기
        val member = memberRepository.findByNameAndEmail(name, email)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code,
                    "해당 정보와 일치하는 회원이 없습니다."
                )
            }

        // 3. 새 비밀번호와 확인 비밀번호가 제공되었는지 확인
        val newPassword = request.newPassword?.takeIf { it.isNotBlank() }
            ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "새 비밀번호를 입력해주세요.")

        val confirmPassword = request.confirmPassword?.takeIf { it.isNotBlank() }
            ?: throw ServiceException(ResultCode.BAD_REQUEST.code, "확인 비밀번호를 입력해주세요.")

        // 4. 새 비밀번호와 확인 비밀번호 일치 여부 확인
        if (newPassword != confirmPassword) {
            throw ServiceException(ResultCode.BAD_REQUEST.code, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.")
        }

        // 5. 새 비밀번호로 업데이트
        member.updatePassword(passwordEncoder.encode(newPassword))
        memberRepository.save(member)
    }
}
