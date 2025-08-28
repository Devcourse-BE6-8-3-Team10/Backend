package com.back.domain.member.dto.response

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role

data class MemberInfoResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val profileUrl: String?
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): MemberInfoResponse {
            return MemberInfoResponse(
                id = member.id,
                email = member.email,
                name = member.name,
                role = member.role,
                profileUrl = member.profileUrl
            )
        }
    }
}
