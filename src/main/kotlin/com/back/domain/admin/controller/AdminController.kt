package com.back.domain.admin.controller

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest
import com.back.domain.admin.dto.response.AdminMemberResponse
import com.back.domain.admin.dto.response.AdminPatentResponse
import com.back.domain.admin.service.AdminService
import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService
) {

    // 전체 회원 목록 조회 API(탈퇴 포함)
    @Operation(summary = "전체 회원 목록 조회", description = "모든 회원 목록을 페이징하여 조회합니다 (탈퇴 포함)")
    @GetMapping("/members")
    fun getAllMembers(
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<RsData<Page<AdminMemberResponse>>> {
        val members = adminService.getAllMembers(pageable)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "회원 목록 조회 성공", members)
        )
    }

    // 회원 상세 조회 API
    @Operation(summary = "회원 상세 조회", description = "회원 ID를 통해 회원 정보를 상세 조회합니다")
    @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다")
    @GetMapping("/members/{memberId}")
    fun getMemberDetail(@PathVariable memberId: Long): ResponseEntity<RsData<AdminMemberResponse>> {
        val response = adminService.getMemberDetail(memberId) // 미존재 시 ServiceException(NotFound) 발생
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "회원 정보 조회 성공", response)
        )
    }

    // 회원 정보 수정 API
    @PatchMapping("/members/{memberId}")
    @Operation(summary = "회원 정보 수정 (관리자)", description = "관리자가 회원 정보를 수정합니다.")
    fun updateMemberByAdmin(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: AdminUpdateMemberRequest
    ): ResponseEntity<RsData<String>> {
        adminService.updateMemberInfo(memberId, request)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "회원 정보 수정 성공")
        )
    }

    // 전체 특허 목록 조회 API
    @Operation(summary = "전체 특허 목록 조회", description = "모든 특허 목록을 페이징하여 조회합니다")
    @GetMapping("/patents")
    fun getAllPatents(
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<RsData<Page<AdminPatentResponse>>> {
        val patents = adminService.getAllPatents(pageable)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "특허 목록 조회 성공", patents)
        )
    }

    // 특허 상세 조회 API
    @Operation(summary = "특허 상세 조회", description = "특허 ID를 통해 특허 정보를 상세 조회합니다")
    @ApiResponse(responseCode = "200", description = "특허 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 특허입니다")
    @GetMapping("/patents/{patentId}")
    fun getPatentDetail(@PathVariable patentId: Long): ResponseEntity<RsData<AdminPatentResponse?>> {
        val response = adminService.getPatentDetail(patentId)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "특허 정보 조회 성공", response)
        )
    }

    // 특허 정보 수정 API
    @PatchMapping("/patents/{patentId}")
    @Operation(summary = "특허 정보 수정 (관리자)", description = "관리자가 특허 정보를 수정합니다.")
    fun updatePatentByAdmin(
        @PathVariable patentId: Long,
        @RequestBody @Valid request: AdminUpdatePatentRequest
    ): ResponseEntity<RsData<String>> {
        adminService.updatePatentInfo(patentId, request)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "특허 정보 수정 성공")
        )
    }

    // 특허 삭제 API
    @DeleteMapping("/patents/{patentId}")
    @Operation(summary = "특허 삭제 (관리자)", description = "관리자가 특허를 삭제합니다.")
    fun deletePatentByAdmin(@PathVariable patentId: Long): ResponseEntity<RsData<String>> {
        adminService.deletePatent(patentId)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "특허 삭제 성공")
        )
    }

    // 회원 탈퇴 API
    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "회원 탈퇴 (관리자)", description = "관리자가 특정 회원을 탈퇴시킵니다.")
    fun deleteMemberByAdmin(@PathVariable memberId: Long): ResponseEntity<RsData<String>> {
        adminService.deleteMember(memberId)
        return ResponseEntity.ok(
            RsData(ResultCode.SUCCESS, "회원 탈퇴 성공")
        )
    }
}