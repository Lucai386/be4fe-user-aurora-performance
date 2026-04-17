package com.be4fe_user_aurora_performance.dto.org;

import java.util.List;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrutturaUtentiResponse {

    private String result;
    private String errorCode;
    private String message;
    private List<UtenteStrutturaDto> utenti;

    public static StrutturaUtentiResponse success(List<UtenteStrutturaDto> utenti) {
        return StrutturaUtentiResponse.builder()
                .result("OK")
                .utenti(utenti)
                .build();
    }

    public static StrutturaUtentiResponse error(ErrorCode errorCode) {
        return StrutturaUtentiResponse.builder()
                .result("KO")
                .errorCode(errorCode.name())
                .message(errorCode.getDefaultMessage())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtenteStrutturaDto {
        private Integer id;
        private String nome;
        private String cognome;
        private String nomeCompleto;
        private String email;
        private String ruolo; // "responsabile" o "staff"
        private String ruoloStruttura; // ruolo specifico nello staff (es. "Collaboratore")
    }
}
