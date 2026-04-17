package com.be4fe_user_aurora_performance.dto.common;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_OK;
import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_KO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String result;
    private String errorCode;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .result(RESULT_OK)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .result(RESULT_OK)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .result(RESULT_KO)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
