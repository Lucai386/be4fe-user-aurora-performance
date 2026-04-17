package com.be4fe_user_aurora_performance.dto.org;

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
public class DeleteStrutturaResponse {

    private String result;
    private String errorCode;

    public static DeleteStrutturaResponse success() {
        return DeleteStrutturaResponse.builder()
                .result(RESULT_OK)
                .build();
    }

    public static DeleteStrutturaResponse error(ErrorCode errorCode) {
        return DeleteStrutturaResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }
}
