package com.back.domain.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String profileUrl;

    @Column(unique = true)
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDateTime deletedAt;

    // 생성자 (빌더)
    @Builder
    public Member(String email, String password, String name, String profileUrl, Role role, Status status) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileUrl = profileUrl;
        this.role = (role != null) ? role : Role.USER;  // 기본 역할은 USER
        this.status = (status != null) ? status : Status.ACTIVE;   // 기본 상태는 ACTIVE
    }

    // 리프레시 토큰을 삭제(무효화)함
    public void removeRefreshToken() {
        this.refreshToken = null;
    }

    // 리프레시 토큰을 설정함
    public void updateRefreshToken(String refreshToken) {
        // 유효성 검사: 리프레시 토큰이 null 또는 빈 문자열이 아닌지 확인
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("리프레시 토큰은 null 또는 빈 문자열일 수 없습니다.");
        }
        this.refreshToken = refreshToken;
    }

    // 회원 탈퇴 (상태 변경)
    public void delete() {
        // 1. 이미 탈퇴한 회원인지 확인
        if (this.status == Status.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        // 2 회원 상태를 DELETED로 변경
        this.status = Status.DELETED;

        // 3. 탈퇴 시간을 현재 시간으로 설정
        this.deletedAt = LocalDateTime.now();
    }


    // 회원 정보 수정 - 시작
    public void updateName(String newName) {
        this.name = newName;
    }

    public void updatePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
    }

    public void updateProfileUrl(String newProfileUrl) {
        this.profileUrl = newProfileUrl;
    }

    public void changeStatus(Status newStatus) {
        this.status = newStatus;
    }
    // 회원 정보 수정 - 끝

}
