package com.be4fe_user_aurora_performance.dto.valutazioni;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO semplificato per selezionare un dipendente dalla lista.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DipendenteSelectDto {
    private Integer id;
    private String nomeCompleto;
    private String email;
    private String ruolo;
    private Integer scorePerformance;
}
