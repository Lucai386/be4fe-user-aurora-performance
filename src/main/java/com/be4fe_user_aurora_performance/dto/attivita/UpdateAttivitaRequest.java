package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Request per aggiornare un'attività esistente.
 */
@Data
public class UpdateAttivitaRequest {
    private String codice;
    private String titolo;
    private String descrizione;
    private String stato;
    private String priorita;
    /** Struttura assegnata */
    private Integer strutturaId;
    /** Se true, rimuove la struttura assegnata */
    private Boolean removeStruttura;
    /** Peso dell'attività per il raggiungimento dell'obiettivo (0-100) */
    private Integer peso;
    private BigDecimal oreStimate;
    private String dataInizio;
    private String dataFineStimata;
    private String dataFineEffettiva;
    private String note;
    private Integer ordine;
}
