package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request per registrare un progressivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistraProgressivoRequest {
    private Long obiettivoId;
    /** Nuovo valore corrente dell'obiettivo */
    private BigDecimal nuovoValore;
    private String note;
}
