package com.back.global.security.auth

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Status
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class MemberDetails(val member: Member) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority("ROLE_${member.getRole()}"))
    }

    override fun getPassword(): String {
        return member.getPassword()
    }

    override fun getUsername(): String {
        return member.getEmail()
    }

    override fun isAccountNonExpired(): Boolean {
        return member.getDeletedAt() == null
    }

    override fun isAccountNonLocked(): Boolean {
        return member.getStatus() != Status.BLOCKED
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return member.getStatus() == Status.ACTIVE
    }
}