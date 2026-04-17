package com.be4fe_user_aurora_performance.dto.valutazioni;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metriche personali per la valutazione di un dipendente.
 * Mostra le performance individuali basate su obiettivi personali e attività assegnate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValutazioniPersonaliDto {
    // Info utente
    private String nomeCompleto;
    private String ruolo;
    private String struttura;
    
    // Obiettivi personali
    private int obiettiviAssegnati;
    private int obiettiviCompletati;
    private int obiettiviInCorso;
    private int obiettiviScaduti;
    private double percentualeObiettiviCompletati;
    private double percentualeMediaObiettivi;
    
    // Attività
    private int attivitaAssegnate;
    private int attivitaCompletate;
    private int attivitaInCorso;
    private int attivitaInRitardo;
    private double percentualeAttivitaCompletate;
    
    // Ore lavorate
    private double oreLavorateSettimana;
    private double oreLavorateMese;
    private double oreLavorateTotali;
    private double oreStimate;
    private double percentualeOreUtilizzate;
    
    // Score performance (0-100, null se non valutabile)
    private Integer scorePerformance;
    private String valutazioneLabel; // "Eccellente", "Buona", "Sufficiente", "Insufficiente"
    
    // Grafici
    private List<ObiettivoProgressoDto> obiettiviProgresso;
    private List<AttivitaPerStatoDto> attivitaPerStato;
    private List<OreLavorateGiornoDto> oreUltimiGiorni;
    private List<PerformanceMensileDto> performanceMensile;
}
