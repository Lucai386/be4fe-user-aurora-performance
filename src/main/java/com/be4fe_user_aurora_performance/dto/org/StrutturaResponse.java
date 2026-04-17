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
public class StrutturaResponse {

    private String result;
    private String errorCode;
    private StrutturaDto struttura;

    public static StrutturaResponse success(StrutturaDto struttura) {
        return StrutturaResponse.builder()
                .result(RESULT_OK)
                .struttura(struttura)
                .build();
    }

    public static StrutturaResponse error(ErrorCode errorCode) {
        return StrutturaResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }
}
