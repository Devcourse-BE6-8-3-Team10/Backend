package com.back.domain.admin.controller;

import com.back.domain.admin.dto.request.AdminUpdateMemberRequest;
import com.back.domain.admin.dto.request.AdminUpdatePatentRequest;
import com.back.domain.admin.dto.response.AdminMemberResponse;
import com.back.domain.admin.dto.response.AdminPatentResponse;
import com.back.domain.admin.service.AdminService;
import com.back.global.rsData.ResultCode;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 전체 회원 목록 조회 API(탈퇴 포함)
    @Operation(summary = "전체 회원 목록 조회", description = "모든 회원 목록을 페이징하여 조회합니다 (탈퇴 포함)")
    @GetMapping("/members")
    public ResponseEntity<RsData<Page<AdminMemberResponse>>> getAllMembers(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AdminMemberResponse> members = adminService.getAllMembers(pageable);

        RsData<Page<AdminMemberResponse>> response =
                new RsData<>(ResultCode.SUCCESS, "회원 목록 조회 성공", members);

        return ResponseEntity.ok(response);
    }

    // 회원 상세 조회 API
    @Operation(summary = "회원 상세 조회", description = "회원 ID를 통해 회원 정보를 상세 조회합니다")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<RsData<AdminMemberResponse>> getMemberDetail(@PathVariable Long memberId) {
        AdminMemberResponse response = adminService.getMemberDetail(memberId);
        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "회원 정보 조회 성공", response));
    }

    // 회원 정보 수정 API
    @PatchMapping("/members/{memberId}")
    @Operation(summary = "회원 정보 수정 (관리자)", description = "관리자가 회원 정보를 수정합니다.")
    public ResponseEntity<RsData<String>> updateMemberByAdmin(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminUpdateMemberRequest request
    ) {
        adminService.updateMemberInfo(memberId, request);
        return ResponseEntity.ok(
                new RsData<>(ResultCode.SUCCESS, "회원 정보 수정 성공")
        );
    }

    // 전체 특허 목록 조회 API
    @Operation(summary = "전체 특허 목록 조회", description = "모든 특허 목록을 페이징하여 조회합니다")
    @GetMapping("/patents")
    public ResponseEntity<RsData<Page<AdminPatentResponse>>> getAllPatents(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AdminPatentResponse> patents = adminService.getAllPatents(pageable);

        RsData<Page<AdminPatentResponse>> response =
                new RsData<>(ResultCode.SUCCESS, "특허 목록 조회 성공", patents);

        return ResponseEntity.ok(response);
    }

    // 특허 상세 조회 API
    @Operation(summary = "특허 상세 조회", description = "특허 ID를 통해 특허 정보를 상세 조회합니다")
    @GetMapping("/patents/{patentId}")
    public ResponseEntity<RsData<AdminPatentResponse>> getPatentDetail(@PathVariable Long patentId) {
        AdminPatentResponse response = adminService.getPatentDetail(patentId);
        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "특허 정보 조회 성공", response));
    }

    // 특허 정보 수정 API
    @PatchMapping("/patents/{patentId}")
    @Operation(summary = "특허 정보 수정 (관리자)", description = "관리자가 특허 정보를 수정합니다.")
    public ResponseEntity<RsData<String>> updatePatentByAdmin(
            @PathVariable Long patentId, @RequestBody @Valid AdminUpdatePatentRequest request) {
        adminService.updatePatentInfo(patentId, request);
        return ResponseEntity.ok(
                new RsData<>(ResultCode.SUCCESS, "특허 정보 수정 성공")
        );
    }

    // 특허 삭제 API
    @DeleteMapping("/patents/{patentId}")
    @Operation(summary = "특허 삭제 (관리자)", description = "관리자가 특허를 삭제합니다.")
    public ResponseEntity<RsData<String>> deletePatentByAdmin(@PathVariable Long patentId) {
        adminService.deletePatent(patentId);
        return ResponseEntity.ok(
                new RsData<>(ResultCode.SUCCESS, "특허 삭제 성공")
        );
    }

    // 회원 탈퇴 API
    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "회원 탈퇴 (관리자)", description = "관리자가 특정 회원을 탈퇴시킵니다.")
    public ResponseEntity<RsData<String>> deleteMemberByAdmin(@PathVariable Long memberId) {
        adminService.deleteMember(memberId);
        return ResponseEntity.ok(
                new RsData<>(ResultCode.SUCCESS, "회원 탈퇴 성공")
        );
    }
} 