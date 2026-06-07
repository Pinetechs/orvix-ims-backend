package com.pinetechs.orvix.ims.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

//@RestControllerAdvice

public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public boolean supports(
            MethodParameter returnType,
            Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body instanceof ApiResponse) {
            return body;
        }

        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();

            if (status >= 400) {
                return ApiResponse.failed(extractErrorMessage(body));
            }
        }

        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(ApiResponse.success(body));
            } catch (Exception e) {
                return body;
            }
        }

        return ApiResponse.success(body);
    }

    private String extractErrorMessage(Object body) {

        if (body instanceof Map<?, ?> map) {
            Object message = map.get("message");
            Object error = map.get("error");

            if (message != null && !message.toString().isBlank()) {
                return message.toString();
            }

            if (error != null) {
                return error.toString();
            }
        }

        return "Request failed";
    }
}