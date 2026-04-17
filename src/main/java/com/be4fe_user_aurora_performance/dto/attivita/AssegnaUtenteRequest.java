package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Request per assegnare un utente a un'attività.
 */
@Data
public class AssegnaUtenteRequest {
    private Long attivitaId;
    private Integer utenteId;
    private String ruolo;
    private BigDecimal oreStimate;
    private String dataInizio;
    private String dataFine;
    private String note;
}
