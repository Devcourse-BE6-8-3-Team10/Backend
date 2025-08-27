package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

public record RsData<T>(
        @NonNull String resultCode,
        @JsonIgnore int statusCode,
        @NonNull String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public RsData(String resultCode, String msg, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), msg, data);
    }

    // enum 기반 생성자 추가
    public RsData(ResultCode resultCode) {
        this(resultCode.code(), resultCode.status(), resultCode.message(), null);
    }

    public RsData(ResultCode resultCode, T data) {
        this(resultCode.code(), resultCode.status(), resultCode.message(), data);
    }

    public RsData(ResultCode resultCode, String customMsg) {
        this(resultCode.code(), resultCode.status(), customMsg, null);
    }

    public RsData(ResultCode resultCode, String customMsg, T data) {
        this(resultCode.code(), resultCode.status(), customMsg, data);
    }
}
