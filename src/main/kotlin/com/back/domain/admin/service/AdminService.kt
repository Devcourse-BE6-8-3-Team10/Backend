package com.back.domain.admin.service

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest
import com.back.domain.admin.dto.response.AdminMemberResponse
import com.back.domain.admin.dto.response.AdminPatentResponse
import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.repository.MemberRepository
import com.back.domain.post.repository.PostRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.ResultCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private val log = LoggerFactory.getLogger(AdminService::class.java)
    }

    // 전체 회원 목록 조회(관리자 제외)
    fun getAllMembers(pageable: Pageable): Page<AdminMemberResponse> {
        log.info("전체 회원 목록 조회 요청 (관리자 제외)")

        return memberRepository.findAllByRoleNot(Role.ADMIN, pageable)
            .map { member -> AdminMemberResponse.fromEntity(member) }
    }

    // 회원 상세 조회
    fun getMemberDetail(memberId: Long): AdminMemberResponse {
        log.info("회원 상세 조회 요청 - memberId: {}", memberId)

        val member = findMemberById(memberId)
        return AdminMemberResponse.fromEntity(member)
    }

    // 회원 정보 수정
    @Transactional
    fun updateMemberInfo(memberId: Long, request: AdminUpdateMemberRequest) {
        log.info("회원 정보 수정 요청 - memberId: {}", memberId)

        val member = findMemberById(memberId)

        try {
            member.updateName(request.name)
            member.changeStatus(request.status)
            updateProfileUrlIfPresent(member, request.profileUrl)
        } catch (e: IllegalStateException) {
            throw ServiceException("400", e.message!!)
        }
    }

    // 전체 특허 목록 조회
    fun getAllPatents(pageable: Pageable): Page<AdminPatentResponse> {
        log.info("전체 특허 목록 조회 요청")
        return postRepository.findAll(pageable)
            .map { post -> AdminPatentResponse.fromEntity(post) }
    }

    // 특허 상세 조회
    fun getPatentDetail(patentId: Long): AdminPatentResponse {
        log.info("특허 상세 조회 요청 - patentId: {}", patentId)
        val post = postRepository.findById(patentId)
            .orElseThrow {
                ServiceException(
                    ResultCode.POST_NOT_FOUND.code(),
                    "존재하지 않는 특허입니다."
                )
            }

        return AdminPatentResponse.fromEntity(post)
    }

    // 특허 정보 수정 (DTO 유효성 검사로 간소화)
    @Transactional
    fun updatePatentInfo(patentId: Long, request: AdminUpdatePatentRequest) {
        log.info("특허 정보 수정 요청 - patentId: {}", patentId)

        val post = postRepository.findById(patentId)
            .orElseThrow {
                ServiceException(
                    ResultCode.POST_NOT_FOUND.code(),
                    "해당 특허가 존재하지 않습니다."
                )
            }

        // 엔티티 업데이트
        post.updatePost(request.title, request.description, request.category, request.price)
        post.updateStatus(request.status)
    }

    // 특허 삭제
    @Transactional
    fun deletePatent(patentId: Long) {
        log.info("특허 삭제 요청 - patentId: {}", patentId)

        val post = postRepository.findById(patentId)
            .orElseThrow {
                ServiceException(
                    ResultCode.POST_NOT_FOUND.code(),
                    "해당 특허가 존재하지 않습니다."
                )
            }

        postRepository.delete(post)
    }

    // 회원 탈퇴 (관리자)
    @Transactional
    fun deleteMember(memberId: Long) {
        log.info("회원 탈퇴 요청 - memberId: {}", memberId)

        val member = findMemberById(memberId)
        member.delete()
    }

    // === 헬퍼 메서드들 ===
    private fun findMemberById(memberId: Long): Member {
        return memberRepository.findById(memberId)
            .orElseThrow {
                ServiceException(
                    ResultCode.MEMBER_NOT_FOUND.code(),
                    "존재하지 않는 회원입니다."
                )
            }
    }

    private fun updateProfileUrlIfPresent(member: Member, profileUrl: String?) {
        when {
            profileUrl.isNullOrBlank() -> {
                // null이거나 빈 문자열인 경우 null로 설정
                if (profileUrl != null) member.updateProfileUrl(null)
            }
            else -> {
                // 유효한 값이 있는 경우 trim해서 설정
                member.updateProfileUrl(profileUrl.trim())
            }
        }
    }
}
