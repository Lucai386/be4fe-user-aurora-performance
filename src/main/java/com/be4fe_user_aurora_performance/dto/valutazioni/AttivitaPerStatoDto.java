package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conteggio attività per stato per i grafici.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttivitaPerStatoDto {
    private String stato;
    private int count;
}
