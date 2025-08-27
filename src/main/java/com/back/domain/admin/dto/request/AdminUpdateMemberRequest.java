package com.back.domain.admin.dto.request;

import com.back.domain.member.entity.Status;

public record AdminUpdateMemberRequest(
        String name,
        Status status,
        String profileUrl
) {}