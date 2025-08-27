package com.back.domain.member.entity;

public enum Status {
    ACTIVE,     // 정상 사용 중
    DELETED,    // 회원 탈퇴 또는 삭제
    BLOCKED     // 운영자에 의해 차단됨
}
