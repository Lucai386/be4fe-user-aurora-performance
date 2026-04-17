package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request per aggiornare un obiettivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateObiettivoRequest {
    private Long id;
    private String titolo;
    private String descrizione;
    private String unitaMisura;
    private String tipo;
    private String stato;
    private BigDecimal valoreIniziale;
    private BigDecimal valoreTarget;
    private BigDecimal peso;
    private String dataInizio;
    private String dataFine;
    private Long utenteAssegnatoId;
}
