package com.be4fe_user_aurora_performance.dto.dup;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import static com.be4fe_user_aurora_performance.enums.AppConstants.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDupResponse {

    private String result;
    private String errorCode;
    private String message;

    public static DeleteDupResponse success() {
        return DeleteDupResponse.builder()
                .result(RESULT_OK)
                .build();
    }

    public static DeleteDupResponse error(ErrorCode errorCode) {
        return DeleteDupResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(errorCode.getDefaultMessage())
                .build();
    }
}
