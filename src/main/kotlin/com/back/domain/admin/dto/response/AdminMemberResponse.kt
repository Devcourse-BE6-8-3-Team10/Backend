package com.back.domain.admin.dto.response

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.entity.Status
import java.time.LocalDateTime

data class AdminMemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val profileUrl: String?,
    val status: Status,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime,
    val deletedAt: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): AdminMemberResponse {
            return AdminMemberResponse(
                id = member.id,
                email = member.email,
                name = member.name,
                role = member.role,
                profileUrl = member.profileUrl,
                status = member.status,
                createdAt = member.createdAt,
                modifiedAt = member.modifiedAt,
                deletedAt = member.deletedAt
            )
        }
    }
}