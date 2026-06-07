package com.pinetechs.orvix.ims.common;

public class ApiResponse<T> {

    private boolean success;

    private String message;

    private T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message);
    }

    public static ApiResponse<Void> failed(String message) {
        return new ApiResponse<>(false, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}