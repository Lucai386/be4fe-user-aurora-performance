package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ore lavorate per giorno per i grafici.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OreLavorateGiornoDto {
    private String data;
    private double ore;
}
