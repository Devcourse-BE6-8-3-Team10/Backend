package com.back.domain.member.repository

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByName(name: String): Optional<Member>
    fun findByNameAndEmail(name: String, email: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
    fun existsByNameAndEmail(name: String, email: String): Boolean
    fun findAllByRoleNot(role: Role, pageable: Pageable): Page<Member>
}
