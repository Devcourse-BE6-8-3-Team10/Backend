package com.back.global.rsData;

public enum ResultCode {

    // ----------------------- [200: 성공] -----------------------
    SUCCESS("200", 200, "성공"),

    // ----------------------- [400: 잘못된 요청] -----------------------
    BAD_REQUEST("400", 400, "잘못된 요청입니다."),
    VALIDATION_ERROR("400-1", 400, "입력값 검증에 실패했습니다."),
    DUPLICATE_EMAIL("400-2", 400, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD("400-3", 400, "비밀번호가 일치하지 않습니다."),
    FILE_UPLOAD_FAIL("400-4", 400, "파일 업로드/수정 실패"),
    FILE_DELETE_FAIL("400-5", 400, "파일 삭제 실패"),

    // ----------------------- [401: 인증 오류] -----------------------
    UNAUTHORIZED("401", 401, "인증이 필요합니다."),
    INVALID_CREDENTIALS("401-1", 401, "이메일 또는 비밀번호가 잘못되었습니다."),
    TOKEN_EXPIRED("401-2", 401, "토큰이 만료되었습니다."),
    INVALID_TOKEN("401-3", 401, "유효하지 않은 토큰입니다."),

    // ----------------------- [403: 권한 오류] -----------------------
    FORBIDDEN("403", 403, "접근 권한이 없습니다."),
    WITHDRAWN_MEMBER("403-1", 403, "탈퇴한 회원입니다."),
    BLOCKED_MEMBER("403-2", 403, "관리자에 의해 정지된 계정입니다."),

    // ----------------------- [404: 리소스 없음] -----------------------
    NOT_FOUND("404", 404, "요청한 리소스를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND("404-1", 404, "존재하지 않는 회원입니다."),
    POST_NOT_FOUND("404-2", 404, "존재하지 않는 특허입니다."),

    // ----------------------- [500: 서버 오류] -----------------------
    SERVER_ERROR("500", 500, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final int status;
    private final String message;

    ResultCode(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}