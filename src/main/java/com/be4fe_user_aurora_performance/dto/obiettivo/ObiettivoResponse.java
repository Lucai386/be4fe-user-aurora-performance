package com.be4fe_user_aurora_performance.dto.obiettivo;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response per operazioni su singolo obiettivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObiettivoResponse {
    private String result;
    private ErrorCode errorCode;
    private ObiettivoDto obiettivo;

    public static ObiettivoResponse ok(ObiettivoDto obiettivo) {
        return ObiettivoResponse.builder()
                .result("OK")
                .obiettivo(obiettivo)
                .build();
    }

    public static ObiettivoResponse ok() {
        return ObiettivoResponse.builder()
                .result("OK")
                .build();
    }

    public static ObiettivoResponse error(ErrorCode errorCode) {
        return ObiettivoResponse.builder()
                .result("KO")
                .errorCode(errorCode)
                .build();
    }
}
