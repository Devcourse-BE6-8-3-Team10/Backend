package com.back.domain.member.dto.response

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import java.time.LocalDateTime

data class MemberMyPageResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val profileUrl: String?,
    val status: Status,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): MemberMyPageResponse {
            return MemberMyPageResponse(
                id = member.id,
                email = member.email,
                name = member.name,
                role = member.role,
                profileUrl = member.profileUrl,
                status = member.status,
                createdAt = member.createdAt,
                updatedAt = member.modifiedAt
            )
        }
    }
}