package com.be4fe_user_aurora_performance.dto.attivita;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per un'attività completa con pesi, metriche e assegnazioni.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttivitaDto {
    private Long id;
    private Long progettoId;
    private String progettoTitolo;
    private String codice;
    private String titolo;
    private String descrizione;
    private String stato;
    private String priorita;
    private Integer strutturaId;
    private String strutturaNome;
    private AttivitaPesiDto pesi;
    private MetricaTemporaleDto metricaTemporale;
    private List<AttivitaAssegnazioneDto> assegnazioni;
    private List<AttivitaStepDto> steps;
    private String createdAt;
    private String updatedAt;
    private String note;
    private Integer ordine;
}
