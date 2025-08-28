package com.back.domain.member.dto.response

import com.back.domain.member.entity.Member

data class OtherMemberInfoResponse(
    val name: String,
    val profileUrl: String?
) {
    companion object {
        @JvmStatic
        fun fromEntity(member: Member): OtherMemberInfoResponse {
            return OtherMemberInfoResponse(
                name = member.name,
                profileUrl = member.profileUrl
            )
        }
    }
}
