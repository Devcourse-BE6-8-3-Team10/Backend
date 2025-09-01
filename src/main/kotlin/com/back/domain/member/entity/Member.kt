package com.back.domain.member.entity

import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDateTime

@Entity
class Member @JvmOverloads constructor(
    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true)
    var profileUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Status = Status.ACTIVE
) : BaseEntity() {

    @Column(unique = true)
    var refreshToken: String? = null
        private set

    var deletedAt: LocalDateTime? = null
        private set

    // 기본 생성자 (JPA 필요)
    protected constructor() : this("", "", "", null, Role.USER, Status.ACTIVE)

    // 리프레시 토큰을 삭제(무효화)함
    fun removeRefreshToken() {
        this.refreshToken = null
    }

    // 리프레시 토큰을 설정함
    fun updateRefreshToken(refreshToken: String) {
        require(refreshToken.trim().isNotEmpty()) { "리프레시 토큰은 null 또는 빈 문자열일 수 없습니다." }
        this.refreshToken = refreshToken
    }

    // 회원 탈퇴 (상태 변경)
    fun delete() {
        // 1. 이미 탈퇴한 회원인지 확인
        check(this.status != Status.DELETED) { "이미 탈퇴한 회원입니다." }

        // 2 회원 상태를 DELETED로 변경
        this.status = Status.DELETED

        // 3. 탈퇴 시간을 현재 시간으로 설정
        this.deletedAt = LocalDateTime.now()
    }


    // 회원 정보 수정 - 시작
    fun updateName(newName: String) {
        this.name = newName
    }

    fun updatePassword(encodedNewPassword: String) {
        this.password = encodedNewPassword
    }

    fun updateProfileUrl(newProfileUrl: String?) {
        this.profileUrl = newProfileUrl
    }

    fun changeStatus(newStatus: Status) {
        // 탈퇴한 회원은 다른 상태로 변경 불가
        check(this.status != Status.DELETED) { "탈퇴한 회원의 상태는 변경할 수 없습니다." }

        this.status = newStatus
    } // 회원 정보 수정 - 끝
}