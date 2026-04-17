package com.be4fe_user_aurora_performance.dto.org;

import java.util.List;

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
public class OrgResponse {

    private String result;
    private String errorCode;
    private List<OrgRowDto> rows;
    private List<List<String>> levels;

    public static OrgResponse success(List<OrgRowDto> rows, List<List<String>> levels) {
        return OrgResponse.builder()
                .result(RESULT_OK)
                .rows(rows)
                .levels(levels)
                .build();
    }

    public static OrgResponse error(ErrorCode errorCode, String message) {
        return OrgResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }
}
