package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per l'assegnazione di un utente a un'attività.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttivitaAssegnazioneDto {
    private Long id;
    private Long attivitaId;
    private Integer utenteId;
    private String utenteNome;
    private String utenteCognome;
    private String ruolo;
    private BigDecimal oreStimate;
    private BigDecimal oreLavorate;
    private String dataAssegnazione;
    private String dataInizio;
    private String dataFine;
    private String note;
}
