package com.be4fe_user_aurora_performance.dto.dup;

import java.util.List;

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
public class DupResponse {

    private String result;
    private String errorCode;
    private String message;
    private DupDto dup;

    public static DupResponse success(DupDto dup) {
        return DupResponse.builder()
                .result(RESULT_OK)
                .dup(dup)
                .build();
    }

    public static DupResponse error(ErrorCode errorCode) {
        return DupResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(errorCode.getDefaultMessage())
                .build();
    }

    public static DupResponse error(ErrorCode errorCode, String message) {
        return DupResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(message)
                .build();
    }
}
