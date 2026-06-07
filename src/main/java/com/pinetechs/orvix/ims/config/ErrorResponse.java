package com.pinetechs.orvix.ims.config;

import java.util.List;

public class ErrorResponse {
    private String message;
    private List<String> errors;

    public ErrorResponse(String message, List<String> errors) {
        this.message = message;
        this.errors = errors;
    }
    public ErrorResponse(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
