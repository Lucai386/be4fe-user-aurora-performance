package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.Builder;
import lombok.Data;

/**
 * DTO per il calcolo dello score performance.
 * Raggruppa tutti i parametri necessari per il calcolo dinamico.
 */
@Data
@Builder
public class ScoreCalcoloDto {
    
    // Obiettivi
    private int obiettiviTotali;
    private int obiettiviCompletati;
    private double percentualeObiettiviCompletati;
    private double percentualeMediaObiettivi;
    
    // Attività
    private int attivitaTotali;
    private int attivitaCompletate;
    private int attivitaInRitardo;
    private double percentualeAttivitaCompletate;
    
    // Ore (opzionale, per future implementazioni)
    private double oreLavorate;
    private double oreStimate;
}
