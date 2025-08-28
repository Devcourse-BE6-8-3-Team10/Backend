package com.back.global.rq;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import com.back.global.security.auth.MemberDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
  // 요청마다 Rq 인스턴스를 새로 생성해 상태를 분리
@RequiredArgsConstructor
public class Rq {

    private final HttpServletRequest request;

    // 현재 로그인된 사용자의 Member 객체를 반환
    public Member getMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberDetails)) return null;

        return ((MemberDetails) auth.getPrincipal()).getMember();
    }

    // 현재 로그인된 사용자의 ID를 반환
    public Long getMemberId() {
        Member member = getMember();
        return (member != null) ? member.getId() : null;
    }

    // 현재 사용자의 로그인 상태 여부를 반환
    public boolean isLogin() {
        return getMember() != null;
    }

    // 현재 사용자의 역할(Role)이 ADMIN인지 여부를 반환
    public boolean isAdmin() {
        Member member = getMember();
        return member != null && member.getRole() == Role.ADMIN;
    }
}