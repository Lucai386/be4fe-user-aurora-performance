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
public class ListDupResponse {

    private String result;
    private String errorCode;
    private String message;
    private List<DupDto> dupList;

    public static ListDupResponse success(List<DupDto> dupList) {
        return ListDupResponse.builder()
                .result(RESULT_OK)
                .dupList(dupList)
                .build();
    }

    public static ListDupResponse error(ErrorCode errorCode) {
        return ListDupResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(errorCode.getDefaultMessage())
                .build();
    }
}
