package com.be4fe_user_aurora_performance.dto.valutazioni;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metriche della struttura per la valutazione da parte del responsabile.
 * Mostra le performance aggregate della struttura/ufficio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValutazioniStrutturaDto {
    // Info struttura
    private Integer strutturaId;
    private String strutturaNome;
    private String responsabileNome;
    private int numeroDipendenti;
    
    // Obiettivi struttura
    private int obiettiviTotali;
    private int obiettiviCompletati;
    private int obiettiviInCorso;
    private int obiettiviScaduti;
    private double percentualeObiettiviCompletati;
    private double percentualeMediaObiettivi;
    
    // Attività struttura
    private int attivitaTotali;
    private int attivitaCompletate;
    private int attivitaInCorso;
    private int attivitaInRitardo;
    private double percentualeAttivitaCompletate;
    
    // Ore
    private double oreLavorateTotali;
    private double oreStimate;
    private double percentualeOreUtilizzate;
    
    // Score performance struttura (0-100, null se non valutabile)
    private Integer scorePerformance;
    private String valutazioneLabel;
    
    // Grafici
    private List<DipendentePerformanceDto> performanceDipendenti;
    private List<ObiettivoProgressoDto> obiettiviProgresso;
    private List<AttivitaPerStatoDto> attivitaPerStato;
    private List<OreLavorateGiornoDto> oreUltimiGiorni;
    private List<PerformanceMensileDto> performanceMensile;
}
