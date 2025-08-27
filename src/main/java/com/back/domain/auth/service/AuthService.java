package com.back.domain.auth.service;

import com.back.domain.auth.dto.request.MemberLoginRequest;
import com.back.domain.auth.dto.request.TokenReissueRequest;
import com.back.domain.auth.dto.response.MemberLoginResponse;
import com.back.domain.auth.dto.response.TokenReissueResponse;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.ResultCode;
import com.back.global.security.auth.MemberDetails;
import com.back.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인
    @Transactional
    public MemberLoginResponse login(MemberLoginRequest request) {
        try {
            // 1. 인증 시도
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(request.email(), request.password());

            Authentication authentication = authenticationManager.authenticate(authToken);

            // 2. 인증 성공시 사용자 정보 로드 (authentication에서 직접 꺼내기)
            MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
            Member member = memberDetails.getMember();

            // 3. JWT 생성
            String accessToken = jwtTokenProvider.generateAccessToken(member);
            String refreshToken = jwtTokenProvider.generateRefreshToken(member);

            // 4. 리프레시 토큰 저장
            member.updateRefreshToken(refreshToken);
            try {
                memberRepository.save(member);
            } catch (DataIntegrityViolationException e) {
                // 리프레시 토큰 저장 실패 시 재시도 또는 예외 처리
                log.error("리프레시 토큰 저장 실패", e);
                throw new ServiceException(ResultCode.SERVER_ERROR.code(), "토큰 저장에 실패했습니다.");
            }

            // 5. DTO 응답 반환
            return new MemberLoginResponse(accessToken, refreshToken, MemberInfoResponse.fromEntity(member));
        } catch (BadCredentialsException e) {
            throw new ServiceException(ResultCode.INVALID_CREDENTIALS.code(), "이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    // 로그아웃
    @Transactional
    public void logout(Member member) {
        member.removeRefreshToken();
        memberRepository.save(member);
    }

    // Access Token 재발급
    @Transactional
    public TokenReissueResponse reissueAccessToken(TokenReissueRequest request) {
        String refreshToken = request.refreshToken();

        // 1. 토큰 유효성 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ServiceException(ResultCode.INVALID_TOKEN.code(), "유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 3. 사용자 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException(ResultCode.MEMBER_NOT_FOUND.code(), "사용자를 찾을 수 없습니다."));

        // 4. 저장된 리프레시 토큰과 일치하는지 확인
        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new ServiceException(ResultCode.INVALID_TOKEN.code(), "토큰이 서버와 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(member);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member);

        // 6. 리프레시 토큰도 갱신
        member.updateRefreshToken(newRefreshToken);
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            log.error("리프레시 토큰 갱신 실패", e);
            throw new ServiceException(ResultCode.SERVER_ERROR.code(), "토큰 갱신에 실패했습니다.");
        }

        return new TokenReissueResponse(newAccessToken, newRefreshToken);
    }
}