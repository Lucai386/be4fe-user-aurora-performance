package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance mensile per i grafici trend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMensileDto {
    private String mese; // "2026-01"
    private Integer score;
    private double percentualeObiettivi;
    private double percentualeAttivita;
}
