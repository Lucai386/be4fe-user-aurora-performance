package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.util.List;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response per lista obiettivi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListObiettiviResponse {
    private String result;
    private ErrorCode errorCode;
    private List<ObiettivoDto> obiettivi;

    public static ListObiettiviResponse ok(List<ObiettivoDto> obiettivi) {
        return ListObiettiviResponse.builder()
                .result("OK")
                .obiettivi(obiettivi)
                .build();
    }

    public static ListObiettiviResponse error(ErrorCode errorCode) {
        return ListObiettiviResponse.builder()
                .result("KO")
                .errorCode(errorCode)
                .build();
    }
}
