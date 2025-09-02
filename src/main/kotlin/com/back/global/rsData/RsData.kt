package com.back.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

data class RsData<T>(
    val resultCode: String,
    @JsonIgnore val statusCode: Int,
    val msg: String,
    val data: T?
) {
    constructor(resultCode: String, message: String, data: T? = null) : this(
        resultCode,
        resultCode.substringBefore("-", resultCode)
            .toIntOrNull() ?: 500, // fallback: 500(서버 오류) 또는 400 등 정책에 맞춰 조정
        message,
        data
    )

    // enum 기반 생성자들
    constructor(resultCode: ResultCode) : this(
        resultCode.code,
        resultCode.status,
        resultCode.defaultMessage,
        null
    )

    constructor(resultCode: ResultCode, data: T?) : this(
        resultCode.code,
        resultCode.status,
        resultCode.defaultMessage,
        data
    )

    constructor(resultCode: ResultCode, customMessage: String) : this(
        resultCode.code,
        resultCode.status,
        customMessage,
        null
    )

    constructor(resultCode: ResultCode, customMessage: String, data: T?) : this(
        resultCode.code,
        resultCode.status,
        customMessage,
        data
    )
}
