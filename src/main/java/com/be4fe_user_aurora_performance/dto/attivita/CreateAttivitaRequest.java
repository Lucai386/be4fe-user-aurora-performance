package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Request per creare una nuova attività.
 */
@Data
public class CreateAttivitaRequest {
    private Long progettoId;
    private String codice;
    private String titolo;
    private String descrizione;
    private String priorita;
    /** Struttura assegnata (opzionale, eredita dal progetto se null) */
    private Integer strutturaId;
    /** Peso dell'attività per il raggiungimento dell'obiettivo (0-100) */
    private Integer peso;
    private BigDecimal oreStimate;
    private String dataInizio;
    private String dataFineStimata;
    private String note;
    private Integer ordine;
}
