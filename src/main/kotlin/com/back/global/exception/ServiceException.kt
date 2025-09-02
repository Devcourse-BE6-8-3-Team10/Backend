package com.back.global.exception

import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData

class ServiceException : RuntimeException {
    val resultCode: String
    val msg: String

    // 기존 생성자 (String, String)
    constructor(resultCode: String, msg: String) : super("$resultCode : $msg") {
        this.resultCode = resultCode
        this.msg = msg
    }

    // 새로운 생성자 (ResultCode, String)
    constructor(resultCode: ResultCode, msg: String) : super("${resultCode.code} : $msg") {
        this.resultCode = resultCode.code
        this.msg = msg
    }

    // ResultCode만 받는 생성자 (기본 메시지 사용)
    constructor(resultCode: ResultCode) : super("${resultCode.code} : ${resultCode.defaultMessage}") {
        this.resultCode = resultCode.code
        this.msg = resultCode.defaultMessage
    }

    val rsData: RsData<Nothing?> get() = RsData(resultCode, msg, null)
}