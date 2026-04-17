package com.be4fe_user_aurora_performance.dto.admin;

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
public class DeleteUserResponse {
    private String result;
    private String errorCode;
    private String message;

    public static DeleteUserResponse success() {
        return DeleteUserResponse.builder()
                .result(RESULT_OK)
                .build();
    }

    public static DeleteUserResponse error(ErrorCode errorCode, String message) {
        return DeleteUserResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(message)
                .build();
    }
}
