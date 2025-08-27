package com.back.global.exception;


import com.back.global.rsData.RsData;

public class ServiceException extends RuntimeException {
    private final String resultCode;
    private final String msg;

    public ServiceException(String resultCode, String msg) {
        super(resultCode + " : " + msg);
        this.resultCode = resultCode;
        this.msg = msg;
    }

    // 예외 발생 시 클라이언트에게 전달할 ResultCode 반환
    public String getResultCode() {
        return resultCode;
    }

    // 예외 발생 시 클라이언트에게 전달할 Message 반환
    public String getMsg() {
        return msg;
    }

    public RsData<Void> getRsData() {
        return new RsData<>(resultCode, msg, null);
    }
}
