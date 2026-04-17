package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le metriche temporali di un'attività.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaTemporaleDto {
    private BigDecimal oreStimate;
    private BigDecimal oreLavorate;
    private BigDecimal oreMancanti;
    /** Percentuale di completamento (gestita manualmente, 0-100) */
    private Integer percentualeCompletamento;
    /** Percentuale di ore lavorate rispetto alle stimate (può superare 100%) */
    private Integer percentualeOreLavorate;
    private String dataInizio;
    private String dataFineStimata;
    private String dataFineEffettiva;
    private Integer scostamentoGiorni;
}
