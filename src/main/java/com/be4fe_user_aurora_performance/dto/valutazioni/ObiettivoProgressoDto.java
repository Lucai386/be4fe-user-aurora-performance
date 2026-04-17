package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progresso di un obiettivo per i grafici.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObiettivoProgressoDto {
    private Long id;
    private String titolo;
    private String stato;
    private double valoreIniziale;
    private double valoreTarget;
    private double valoreCorrente;
    private double percentuale;
    private int peso;
}
