package com.ksyun.course;

public class ApiResponse {
    private String msg;
    private int code;
    private String requestId;
    private String data;

    // getters and settersf

    public String getMsg() {
        return msg;
    }

    public int getCode() {
        return code;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getData() {
        return data;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ApiResponse(String msg, int code, String requestId, String data) {
        this.msg = msg;
        this.code = code;
        this.requestId = requestId;
        this.data = data;
    }
}