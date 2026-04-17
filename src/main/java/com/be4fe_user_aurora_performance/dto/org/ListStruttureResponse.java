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
public class ListStruttureResponse {

    private String result;
    private String errorCode;
    private List<StrutturaDto> strutture;

    public static ListStruttureResponse success(List<StrutturaDto> strutture) {
        return ListStruttureResponse.builder()
                .result(RESULT_OK)
                .strutture(strutture)
                .build();
    }

    public static ListStruttureResponse error(ErrorCode errorCode) {
        return ListStruttureResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }
}
