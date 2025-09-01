package com.back.global.security.auth

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Status
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class MemberDetails(val member: Member) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority("ROLE_${member.role}"))
    }

    override fun getPassword(): String {
        return member.password
    }

    override fun getUsername(): String {
        return member.email
    }

    override fun isAccountNonExpired(): Boolean {
        return member.deletedAt == null
    }

    override fun isAccountNonLocked(): Boolean {
        return member.status != Status.BLOCKED
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return member.status == Status.ACTIVE
    }
}