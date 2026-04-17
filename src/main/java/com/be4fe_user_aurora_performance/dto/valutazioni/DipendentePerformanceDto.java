package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance di un singolo dipendente per la vista responsabile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DipendentePerformanceDto {
    private Integer utenteId;
    private String nomeCompleto;
    private String ruolo;
    private int obiettiviCompletati;
    private int obiettiviTotali;
    private int attivitaCompletate;
    private int attivitaTotali;
    private Integer scorePerformance;
    private String valutazioneLabel;
}
