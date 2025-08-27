package com.back.domain.auth.dto.response;

public record TokenReissueResponse(String accessToken, String refreshToken){
}
