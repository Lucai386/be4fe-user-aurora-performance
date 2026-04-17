package com.be4fe_user_aurora_performance.dto.valutazioni;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metriche dettagliate di un singolo dipendente visto dal responsabile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValutazioniDipendenteDto {
    // Info dipendente
    private Integer utenteId;
    private String nomeCompleto;
    private String email;
    private String ruolo;
    
    // Obiettivi
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
    
    // Ore
    private double oreLavorateSettimana;
    private double oreLavorateMese;
    private double oreLavorateTotali;
    private double oreStimate;
    private double percentualeOreUtilizzate;
    
    // Score performance (0-100, null se non valutabile)
    private Integer scorePerformance;
    private String valutazioneLabel;
    
    // Grafici
    private List<ObiettivoProgressoDto> obiettiviProgresso;
    private List<AttivitaPerStatoDto> attivitaPerStato;
    private List<OreLavorateGiornoDto> oreUltimiGiorni;
    private List<PerformanceMensileDto> performanceMensile;
}
