package com.be4fe_user_aurora_performance.dto.attivita;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per un sotto-step di un'attività.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttivitaStepDto {
    private Long id;
    private Long attivitaId;
    private String titolo;
    private String descrizione;
    private Boolean completato;
    /** Peso dello step (0-100), contribuisce alla percentuale di completamento dell'attività */
    private Integer peso;
    private Integer ordine;
}
