package com.back.domain.member.dto.response

import com.back.domain.member.entity.Member

data class MemberInfoResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: String,
    val profileUrl: String?
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): MemberInfoResponse {
            return MemberInfoResponse(
                id = member.getId(),
                email = member.getEmail(),
                name = member.getName(),
                role = member.getRole().name,
                profileUrl = member.getProfileUrl()
            )
        }
    }
}
