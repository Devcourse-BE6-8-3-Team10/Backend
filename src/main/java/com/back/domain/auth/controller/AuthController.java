package com.back.domain.auth.controller;

import com.back.domain.auth.dto.request.MemberLoginRequest;
import com.back.domain.auth.dto.request.MemberSignupRequest;
import com.back.domain.auth.dto.request.TokenReissueRequest;
import com.back.domain.auth.dto.response.MemberLoginResponse;
import com.back.domain.auth.dto.response.TokenReissueResponse;
import com.back.domain.auth.service.AuthService;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import com.back.global.rsData.ResultCode;
import com.back.global.rsData.RsData;
import com.back.global.security.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;
    
    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;
    
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    // 회원가입 API
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    public ResponseEntity<RsData<String>> signup(@Valid @RequestBody MemberSignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "회원가입 성공"));
    }

    // 로그인 API
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<RsData<MemberLoginResponse>> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletResponse response) {
        // 로그인 요청 처리
        MemberLoginResponse loginResponse = authService.login(request);

        // AccessToken 쿠키 생성 (자동 전송용)
        ResponseCookie accessTokenCookie = createCookie(
                "accessToken",
                loginResponse.accessToken(),
                (int) (accessTokenValidity / 1000), // application.yml과 일치
                false, // 프로덕션 환경에서는 true
                false // prod 환경이면 true로 분기
        );

        // RefreshToken 쿠키 생성 (HttpOnly 쿠키로 설정, 보안용)
        ResponseCookie refreshTokenCookie = createCookie(
                "refreshToken",
                loginResponse.refreshToken(),
                (int) (refreshTokenValidity / 1000), // application.yml과 일치
                true,
                false // 프로덕션 환경에서는 true
        );

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        
        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "로그인 성공", loginResponse));
    }

    // 로그인 사용자 정보 조회 API
    @GetMapping("/me")
    @Operation(summary = "로그인 사용자 정보 조회", description = "유효한 JWT 토큰을 통해 현재 인증된 사용자 정보를 조회합니다.")
    public ResponseEntity<RsData<MemberInfoResponse>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>(ResultCode.UNAUTHORIZED, "로그인된 사용자가 없습니다."));
        }

        MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
        Member member = memberDetails.getMember();
        MemberInfoResponse response = MemberInfoResponse.fromEntity(member);

        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "사용자 정보 조회 성공", response));
    }

    // 로그아웃 API
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자의 refresh 토큰을 삭제합니다.")
    public ResponseEntity<RsData<Void>> logout(
            Authentication authentication,
            HttpServletResponse response) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>(ResultCode.UNAUTHORIZED, "로그인된 사용자가 없습니다."));
        }

        MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
        Member member = memberDetails.getMember();
        authService.logout(member);

        // AccessToken 쿠키 삭제
        ResponseCookie accessTokenCookie = createCookie("accessToken", "", 0, false, false);
        
        // RefreshToken 쿠키 삭제
        ResponseCookie refreshTokenCookie = createCookie("refreshToken", "", 0, true, false);

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "로그아웃 성공", null));
    }

    // Access Token 재발급 API
    @PostMapping("/reissue")
    @Operation(summary = "AccessToken 재발급", description = "RefreshToken으로 AccessToken을 재발급 받습니다.")
    public ResponseEntity<RsData<TokenReissueResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request,
            HttpServletResponse response) {
        TokenReissueResponse reissueResponse = authService.reissueAccessToken(request);
        
        // 새로운 AccessToken을 쿠키에 설정
        ResponseCookie accessTokenCookie = createCookie(
                "accessToken",
                reissueResponse.accessToken(),
                (int) (accessTokenValidity / 1000),
                false,
                false
        );
        
        // 새로운 RefreshToken을 쿠키에 설정
        ResponseCookie refreshTokenCookie = createCookie(
                "refreshToken",
                reissueResponse.refreshToken(),
                (int) (refreshTokenValidity / 1000),
                true,
                false
        );

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        
        return ResponseEntity.ok(new RsData<>(ResultCode.SUCCESS, "토큰 재발급 성공", reissueResponse));
    }

    // 쿠키 생성 메소드
    private ResponseCookie createCookie(String name, String value, int maxAge, boolean httpOnly, boolean secure) {
        return ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Strict")
                .maxAge(maxAge)
                .build();
    }
}
