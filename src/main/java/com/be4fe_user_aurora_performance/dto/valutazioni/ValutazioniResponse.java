package com.be4fe_user_aurora_performance.dto.valutazioni;

import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_OK;
import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_KO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValutazioniResponse {
    private String result;
    private ErrorCode errorCode;
    private String message;
    
    /** Tipo di vista: "personal", "responsabile", "admin" */
    private String viewType;
    
    /** Metriche personali (per dipendente) */
    private ValutazioniPersonaliDto metricsPersonali;
    
    /** Metriche struttura (per responsabile) */
    private ValutazioniStrutturaDto metricsStruttura;
    
    /** Metriche dipendente selezionato (per responsabile) */
    private ValutazioniDipendenteDto metricsDipendente;
    
    /** Lista dipendenti della struttura (per responsabile) */
    private List<DipendenteSelectDto> dipendentiStruttura;

    public static ValutazioniResponse ok() {
        return ValutazioniResponse.builder().result(RESULT_OK).build();
    }

    public static ValutazioniResponse ko(ErrorCode errorCode) {
        return ValutazioniResponse.builder().result(RESULT_KO).errorCode(errorCode).build();
    }
    
    public static ValutazioniResponse ko(ErrorCode errorCode, String message) {
        return ValutazioniResponse.builder().result(RESULT_KO).errorCode(errorCode).message(message).build();
    }
}
