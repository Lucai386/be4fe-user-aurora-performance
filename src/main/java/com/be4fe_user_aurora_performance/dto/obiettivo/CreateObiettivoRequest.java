package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request per creare un nuovo obiettivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateObiettivoRequest {
    private String titolo;
    private String descrizione;
    private String unitaMisura;
    /** CRESCENTE o DECRESCENTE */
    private String tipo;
    private BigDecimal valoreIniziale;
    private BigDecimal valoreTarget;
    private BigDecimal peso;
    private String dataInizio;
    private String dataFine;
    private Integer anno;
    private Integer strutturaId;
    private Long utenteAssegnatoId;
}
